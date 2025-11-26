package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import ovo.sypw.onlineexamsystemback.entity.ExamSubmission
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.SubmissionService
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
    private val objectMapper: ObjectMapper
) : SubmissionService {

    override fun startExam(examId: Long, userId: Long): SubmissionResponse {
        // Check if already started or submitted
        val existingSubmission = submissionRepository.findByExamIdAndUserId(examId, userId)
        if (existingSubmission != null) {
            // Return existing submission
            val exam = examRepository.findById(examId).orElseThrow {
                throw IllegalArgumentException("考试不存在")
            }
            return toSubmissionResponse(existingSubmission, exam.title)
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
        return toSubmissionResponse(savedSubmission, exam.title)
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
        return toSubmissionResponse(savedSubmission, exam.title)
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

        return toSubmissionResponse(submission, exam.title)
    }

    override fun getExamSubmissions(examId: Long, userId: Long, userRole: String): List<SubmissionResponse> {
        // Only teacher/admin can view all exam submissions
        if (userRole != "teacher" && userRole != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以查看考试提交记录")
        }

        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check if teacher owns the exam
        if (userRole == "teacher" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限查看此考试的提交记录")
        }

        val submissions = submissionRepository.findByExamId(examId)
        return submissions.map { toSubmissionResponse(it, exam.title) }
    }

    override fun getUserSubmissions(userId: Long): List<SubmissionResponse> {
        val submissions = submissionRepository.findByUserId(userId)
        return submissions.map { submission ->
            val exam = examRepository.findById(submission.examId).orElseThrow {
                throw IllegalArgumentException("考试不存在")
            }
            toSubmissionResponse(submission, exam.title)
        }
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

        // Add manual scores
        @Suppress("UNCHECKED_CAST")
        val questionScores = submitDetail.getOrPut("questionScores") { 
            mutableMapOf<String, Int>() 
        } as MutableMap<String, Int>
        request.questionScores.forEach { (qid, score) ->
            questionScores[qid.toString()] = score
        }

        // Calculate total score
        val totalScore = questionScores.values.sum()
        submitDetail["totalScore"] = totalScore
        submitDetail["manuallyGraded"] = true

        submission.submitDetail = objectMapper.writeValueAsString(submitDetail)
        submission.submitScore = totalScore
        submission.status = 2 // Graded

        val gradedSubmission = submissionRepository.save(submission)
        return toSubmissionResponse(gradedSubmission, exam.title)
    }

    override fun recordProctoringEvent(request: ProctoringEventRequest, userId: Long): Boolean {
        val examId = request.examId!!
        
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
                }
            }
        }

        submissionRepository.save(submission)
        return submission.status == 1 // Return true if auto-submitted
    }

    /**
     * Auto-grade objective questions
     * Returns: Map with questionScores and totalScore
     */
    private fun autoGradeAnswers(answers: Map<Long, String>, examQuestions: List<ovo.sypw.onlineexamsystemback.entity.ExamQuestion>): MutableMap<String, Any> {
        val questionScores = mutableMapOf<String, Int>()
        var totalScore = 0

        examQuestions.forEach { eq ->
            val question = questionRepository.findById(eq.questionId).orElse(null) ?: return@forEach
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
            println("Warning: Invalid proctoring data JSON, ignoring: ${e.message}")
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

    private fun toSubmissionResponse(submission: ExamSubmission, examTitle: String): SubmissionResponse {
        val user = userRepository.findById(submission.userId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

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
                // Ignore parsing errors
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
            userName = user.realName ?: user.username,
            answers = submission.answers,
            objectiveScore = objectiveScore,
            subjectiveScore = subjectiveScore,
            totalScore = totalScore,
            status = submission.status,
            statusDescription = statusDescription,
            switchCount = submission.switchCount,
            startTime = submission.startTime,
            submitTime = submission.submitTime,
            submitDetail = submission.submitDetail
        )
    }
}
