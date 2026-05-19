package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.ProctoringDataResponse
import ovo.sypw.onlineexamsystemback.dto.response.ProctoringEventResponse
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import ovo.sypw.onlineexamsystemback.entity.ExamSubmission
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.NotificationService
import ovo.sypw.onlineexamsystemback.service.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SubmissionServiceImpl(
    private val submissionRepository: ExamSubmissionRepository,
    private val examRepository: ExamRepository,
    private val examQuestionRepository: ExamQuestionRepository,
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val notificationService: NotificationService
) : SubmissionService {

    private val logger = LoggerFactory.getLogger(SubmissionServiceImpl::class.java)

    override fun startExam(examId: Long, userId: Long): SubmissionResponse {
        // Check if already started or submitted
        val existingSubmission = submissionRepository.findByExamIdAndUserId(examId, userId)
        if (existingSubmission != null) {
            // Return existing submission
            val exam = examRepository.findById(examId).orElseThrow {
                throw IllegalArgumentException("考试不存在")
            }
            val now = LocalDateTime.now()
            if (exam.status != 1) {
                throw IllegalArgumentException("考试未发布或已结束")
            }
            if (now.isBefore(exam.startTime)) {
                throw IllegalArgumentException("考试尚未开始")
            }
            if (now.isAfter(exam.endTime)) {
                throw IllegalArgumentException("考试已结束")
            }
            val user = userRepository.findById(userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
            return toSubmissionResponse(existingSubmission, exam.title, user.realName ?: user.username)
        }

        // Validate exam exists and is published
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        if (exam.status != 1) {
            throw IllegalArgumentException("考试未发布或已结束")
        }

        // Check if exam time is valid
        val now = LocalDateTime.now()
        if (now.isBefore(exam.startTime)) {
            throw IllegalArgumentException("考试尚未开始")
        }
        if (now.isAfter(exam.endTime)) {
            throw IllegalArgumentException("考试已结束")
        }

        // Create new submission (status = 0: in progress)
        val submission = ExamSubmission(
            examId = examId,
            userId = userId,
            status = 0  // In progress
        )

        val savedSubmission = submissionRepository.save(submission)
        val user = userRepository.findById(userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        return toSubmissionResponse(savedSubmission, exam.title, user.realName ?: user.username)
    }

    override fun submitExam(request: SubmissionRequest, userId: Long): SubmissionResponse {
        // Validate exam exists and is published
        val exam = examRepository.findById(request.examId!!).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        if (exam.status != 1) {
            throw IllegalArgumentException("考试未发布或已结束")
        }

        // Check if exam time is valid
        val now = LocalDateTime.now()
        if (now.isBefore(exam.startTime)) {
            throw IllegalArgumentException("考试尚未开始")
        }
        if (now.isAfter(exam.endTime)) {
            throw IllegalArgumentException("考试已结束")
        }

        // Check if already submitted
        val examId = exam.id!!
        val existingSubmission = submissionRepository.findByExamIdAndUserId(examId, userId)
        if (existingSubmission != null && existingSubmission.status >= 1) {
            throw IllegalArgumentException("您已提交过此考试")
        }

        // Get exam questions
        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(examId)

        // Auto-grade objective questions
        val gradeResult = autoGradeAnswers(request.answers, examQuestions)

        // Create or update submission
        val submission = existingSubmission ?: ExamSubmission(
            examId = examId,
            userId = userId
        )

        submission.answers = objectMapper.writeValueAsString(request.answers)
        submission.submitDetail = objectMapper.writeValueAsString(gradeResult)
        submission.submitScore = gradeResult["totalScore"] as? Int
        submission.status = 1 // Submitted
        submission.submitTime = now


        val savedSubmission = submissionRepository.save(submission)
        val user = userRepository.findById(userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        return toSubmissionResponse(savedSubmission, exam.title, user.realName ?: user.username)
    }

    override fun getSubmissionById(id: Long, userId: Long, userRole: String): SubmissionResponse {
        val submission = submissionRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("提交记录不存在")
        }

        // Check permission: student can only view own submission
        if (userRole == "student" && submission.userId != userId) {
            throw IllegalArgumentException("您没有权限查看此提交记录")
        }

        val exam = examRepository.findById(submission.examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }
        val user = userRepository.findById(submission.userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        return toSubmissionResponse(submission, exam.title, user.realName ?: user.username)
    }

    override fun getExamSubmissions(examId: Long, userId: Long, userRole: String, pageable: Pageable): Page<SubmissionResponse> {
        if (userRole != "teacher" && userRole != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以查看考试提交记录")
        }

        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        if (userRole == "teacher" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限查看此考试的提交记录")
        }

        val submissionPage = submissionRepository.findByExamId(examId, pageable)
        val userIds = submissionPage.content.map { it.userId }.toSet()
        val users = userRepository.findAllById(userIds).associateBy { it.id }
        return submissionPage.map { submission ->
            val user = users[submission.userId] ?: throw IllegalArgumentException("用户不存在")
            toSubmissionResponse(submission, exam.title, user.realName ?: user.username)
        }
    }

    override fun getUserSubmissions(userId: Long, pageable: Pageable): Page<SubmissionResponse> {
        val submissionPage = submissionRepository.findByUserId(userId, pageable)
        val examIds = submissionPage.content.map { it.examId }.toSet()
        val exams = examRepository.findAllById(examIds).associateBy { it.id }
        val user = userRepository.findById(userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        return submissionPage.map { submission ->
            val exam = exams[submission.examId] ?: throw IllegalArgumentException("考试不存在")
            toSubmissionResponse(submission, exam.title, user.realName ?: user.username)
        }
    }

    override fun getUserSubmissionByExamId(examId: Long, userId: Long): Page<SubmissionResponse> {
        val submission = submissionRepository.findByExamIdAndUserId(examId, userId)
            ?: return org.springframework.data.domain.PageImpl(emptyList())
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }
        val user = userRepository.findById(userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        val response = toSubmissionResponse(submission, exam.title, user.realName ?: user.username)
        return org.springframework.data.domain.PageImpl(listOf(response))
    }

    override fun gradeSubmission(id: Long, request: GradeRequest, userId: Long, userRole: String): SubmissionResponse {
        val submission = submissionRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("提交记录不存在")
        }

        // Only teacher/admin can grade
        if (userRole != "teacher" && userRole != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以评分")
        }

        val exam = examRepository.findById(submission.examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check if teacher owns the exam
        if (userRole == "teacher" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限为此考试评分")
        }

        // Update submit detail with manual scores
        val submitDetail = if (submission.submitDetail != null) {
            objectMapper.readValue(submission.submitDetail!!, object : TypeReference<MutableMap<String, Any>>() {})
        } else {
            mutableMapOf()
        }

        // Load exam questions first to get max scores
        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(submission.examId)
        val maxScores = examQuestions.associate { it.questionId.toString() to it.score }

        // Add manual scores with per-question validation
        @Suppress("UNCHECKED_CAST")
        val questionScores = submitDetail.getOrPut("questionScores") { 
            mutableMapOf<String, Int>() 
        } as MutableMap<String, Int>
        request.questionScores.forEach { (qid, score) ->
            val maxScore = maxScores[qid.toString()] ?: 1000
            if (score < 0 || score > maxScore) {
                throw IllegalArgumentException("题目 $qid 的分数必须在 0 到 $maxScore 之间")
            }
            questionScores[qid.toString()] = score
        }

        // Calculate objective and subjective scores based on question types
        val questionIds = examQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

        var objectiveScore = 0
        var subjectiveScore = 0
        examQuestions.forEach { eq ->
            val score = questionScores[eq.questionId.toString()] ?: 0
            val question = questions[eq.questionId]
            when (question?.type) {
                "single", "multiple", "true_false" -> objectiveScore += score
                "fill_blank", "short_answer" -> subjectiveScore += score
            }
        }

        // Calculate total score (retains existing behavior including any out-of-exam questionIds)
        val totalScore = questionScores.values.sum()
        submitDetail["totalScore"] = totalScore
        submitDetail["manuallyGraded"] = true
        submitDetail["objectiveScore"] = objectiveScore
        submitDetail["subjectiveScore"] = subjectiveScore

        submission.submitDetail = objectMapper.writeValueAsString(submitDetail)
        submission.submitScore = totalScore
        submission.status = 2 // Graded

        val gradedSubmission = submissionRepository.save(submission)

        // Send grade released notification
        try {
            notificationService.sendGradeReleasedNotification(
                examId = exam.id!!,
                examTitle = exam.title,
                studentId = submission.userId,
                score = totalScore
            )
        } catch (e: Exception) {
            logger.warn("发送成绩通知失败: ${e.message}")
        }

        val user = userRepository.findById(submission.userId).orElseThrow { throw IllegalArgumentException("用户不存在") }
        return toSubmissionResponse(gradedSubmission, exam.title, user.realName ?: user.username)
    }

    override fun recordProctoringEvent(request: ProctoringEventRequest, userId: Long): ProctoringEventResponse {
        val examId = request.examId ?: throw IllegalArgumentException("考试ID不能为空")
        
        // Find or create submission for this user and exam
        var submission = submissionRepository.findByExamIdAndUserId(examId, userId)
        
        if (submission == null) {
            // Validate exam exists and is active
            val exam = examRepository.findById(examId).orElseThrow {
                throw IllegalArgumentException("考试不存在")
            }
            
            if (exam.status != 1) {
                throw IllegalArgumentException("考试未发布或已结束")
            }
            
            val now = LocalDateTime.now()
            if (now.isBefore(exam.startTime) || now.isAfter(exam.endTime)) {
                throw IllegalArgumentException("考试未在进行时间内")
            }
            
            // Create new submission (status = 0: in progress)
            submission = ExamSubmission(
                examId = examId,
                userId = userId,
                status = 0
            )
            submission = submissionRepository.save(submission)
        }

        // Cannot record events for already submitted exams
        if (submission.status >= 1) {
            throw IllegalArgumentException("考试已提交，无法记录监考事件")
        }

        // Increment switch count
        submission.switchCount++

        // Add event to proctoring data
        val event = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "type" to request.eventType,
            "detail" to request.detail
        )

        val proctoringData = if (submission.proctoringData != null) {
            val data = objectMapper.readValue(submission.proctoringData!!, object : TypeReference<MutableMap<String, Any>>() {})
            @Suppress("UNCHECKED_CAST")
            val events = data.getOrPut("events") { 
                mutableListOf<Map<String, String?>>() 
            } as MutableList<Map<String, String?>>
            events.add(event)
            data["switchCount"] = submission.switchCount
            data
        } else {
            mutableMapOf(
                "events" to mutableListOf(event),
                "switchCount" to submission.switchCount
            )
        }

        submission.proctoringData = objectMapper.writeValueAsString(proctoringData)

        // Check if exceeded max switch count
        val exam = examRepository.findById(submission.examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        var autoSubmitted = false
        if (exam.strictMode && exam.maxSwitchCount != null) {
            val maxCount = exam.maxSwitchCount!!
            if (submission.switchCount >= maxCount) {
                // Auto-submit
                if (submission.status == 0) {
                    submission.status = 1
                    submission.submitTime = LocalDateTime.now()
                    proctoringData["autoSubmitted"] = true
                    proctoringData["reason"] = "超出最大切出次数限制"
                    submission.proctoringData = objectMapper.writeValueAsString(proctoringData)
                    autoSubmitted = true
                }
            }
        }

        submissionRepository.save(submission)
        return ProctoringEventResponse(
            recorded = true,
            autoSubmitted = autoSubmitted
        )
    }

    override fun getProctoringData(submissionId: Long, userId: Long, userRole: String): ProctoringDataResponse {
        val submission = submissionRepository.findById(submissionId).orElseThrow {
            throw IllegalArgumentException("提交记录不存在")
        }

        // Permission check
        if (userRole == "student" && submission.userId != userId) {
            throw IllegalArgumentException("您没有权限查看此监考记录")
        }

        val exam = examRepository.findById(submission.examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        if (userRole == "teacher" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限查看此监考记录")
        }

        val proctoringData = if (submission.proctoringData != null) {
            objectMapper.readValue(submission.proctoringData!!, object : TypeReference<Map<String, Any>>() {})
        } else {
            emptyMap()
        }

        return ProctoringDataResponse(
            submissionId = submissionId,
            examId = submission.examId,
            userId = submission.userId,
            switchCount = submission.switchCount,
            proctoringData = proctoringData,
            status = submission.status
        )
    }

    /**
     * Auto-grade objective questions
     * Returns: Map with questionScores and totalScore
     */
    private fun autoGradeAnswers(answers: Map<Long, String>, examQuestions: List<ovo.sypw.onlineexamsystemback.entity.ExamQuestion>): MutableMap<String, Any> {
        val questionIds = examQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

        val questionScores = mutableMapOf<String, Int>()
        var totalScore = 0

        examQuestions.forEach { eq ->
            val question = questions[eq.questionId] ?: return@forEach
            val userAnswer = answers[eq.questionId]?.trim() ?: ""
            val correctAnswer = question.answer?.trim() ?: ""

            val score = when (question.type) {
                "single", "true_false" -> {
                    // Complete match only
                    if (userAnswer.equals(correctAnswer, ignoreCase = true)) eq.score else 0
                }
                "multiple" -> {
                    // Partial scoring for multiple choice
                    gradeMultipleChoice(userAnswer, correctAnswer, eq.score)
                }
                else -> {
                    // fill_blank, short_answer need manual grading
                    0
                }
            }

            questionScores[eq.questionId.toString()] = score
            totalScore += score
        }

        return mutableMapOf(
            "questionScores" to questionScores,
            "totalScore" to totalScore,
            "autoGraded" to true
        )
    }

    /**
     * Grade multiple choice question
     * - All correct: full score
     * - Subset of correct (less selected): half score
     * - Any wrong or extra: 0 score
     */
    private fun gradeMultipleChoice(userAnswer: String, correctAnswer: String, fullScore: Int): Int {
        val userOptions = userAnswer.split(",").map { it.trim() }.toSet()
        val correctOptions = correctAnswer.split(",").map { it.trim() }.toSet()

        // If user selected extra options or wrong options: 0
        if (!correctOptions.containsAll(userOptions)) {
            return 0
        }

        // If all correct: full score
        if (userOptions == correctOptions) {
            return fullScore
        }

        // If subset (less selected): half score
        if (userOptions.isNotEmpty() && correctOptions.containsAll(userOptions)) {
            return fullScore / 2
        }

        return 0
    }

    /**
     * Validate proctoring data is valid JSON or null
     */
    private fun validateProctoringData(data: String?): String? {
        if (data.isNullOrBlank()) {
            return null
        }
        
        return try {
            // Try to parse to verify it's valid JSON
            objectMapper.readValue(data, object : TypeReference<Map<String, Any>>() {})
            data
        } catch (e: Exception) {
            // If invalid JSON, log warning and return null
            logger.warn("Invalid proctoring data JSON, ignoring: ${e.message}")
            null
        }
    }

    private fun updateProctoringData(existingData: String?, message: String): String {
        val data = if (existingData != null) {
            objectMapper.readValue(existingData, object : TypeReference<MutableMap<String, Any>>() {})
        } else {
            mutableMapOf()
        }

        data["autoSubmitMessage"] = message
        return objectMapper.writeValueAsString(data)
    }

    private fun toSubmissionResponse(submission: ExamSubmission, examTitle: String, userName: String): SubmissionResponse {
        // Parse submit detail for scores
        var objectiveScore: Int? = null
        var subjectiveScore: Int? = null
        var totalScore = submission.submitScore

        if (submission.submitDetail != null) {
            try {
                val detail = objectMapper.readValue(submission.submitDetail!!, object : TypeReference<Map<String, Any>>() {})
                totalScore = detail["totalScore"] as? Int ?: submission.submitScore

                // If manually graded, separate objective and subjective scores
                if (detail["manuallyGraded"] as? Boolean == true) {
                    @Suppress("UNCHECKED_CAST")
                    val scores = detail["questionScores"] as? Map<String, Int>
                    // This is simplified - in real scenario you'd need to check question types
                    objectiveScore = detail["objectiveScore"] as? Int
                    subjectiveScore = detail["subjectiveScore"] as? Int
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse submitDetail JSON for submission ${submission.id}, using entity defaults: ${e.message}")
            }
        }

        val statusDescription = when (submission.status) {
            0 -> "答题中"
            1 -> "已提交"
            2 -> "已评分"
            else -> "未知"
        }

        return SubmissionResponse(
            id = submission.id ?: 0L,
            examId = submission.examId,
            examTitle = examTitle,
            userId = submission.userId,
            userName = userName,
            answers = submission.answers,
            objectiveScore = objectiveScore,
            subjectiveScore = subjectiveScore,
            totalScore = totalScore,
            status = submission.status,
            statusDescription = statusDescription,
            switchCount = submission.switchCount,
            startTime = submission.startTime,
            submitTime = submission.submitTime,
            submitDetail = submission.submitDetail,
            proctoringData = submission.proctoringData
        )
    }
}
