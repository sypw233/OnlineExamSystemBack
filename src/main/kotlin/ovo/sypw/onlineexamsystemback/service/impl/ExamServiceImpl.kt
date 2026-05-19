package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.ComposeRandomExamRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.*
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse
import ovo.sypw.onlineexamsystemback.entity.Exam
import ovo.sypw.onlineexamsystemback.entity.ExamQuestion
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.ExamService
import ovo.sypw.onlineexamsystemback.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ExamServiceImpl(
    private val examRepository: ExamRepository,
    private val examQuestionRepository: ExamQuestionRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val submissionRepository: ExamSubmissionRepository,
    private val courseSelectionRepository: CourseSelectionRepository,
    private val questionBankQuestionRepository: QuestionBankQuestionRepository,
    private val notificationService: NotificationService
) : ExamService {

    override fun createExam(examRequest: ExamRequest, creatorId: Long): ExamResponse {
        val creator = userRepository.findById(creatorId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        if (creator.role != "teacher" && creator.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以创建考试")
        }

        val courseId = examRequest.courseId ?: throw IllegalArgumentException("课程ID不能为空")
        val startTime = examRequest.startTime ?: throw IllegalArgumentException("开始时间不能为空")
        val endTime = examRequest.endTime ?: throw IllegalArgumentException("结束时间不能为空")

        // Validate course exists
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Validate time
        if (endTime <= startTime) {
            throw IllegalArgumentException("结束时间必须晚于开始时间")
        }

        // Validate proctoring settings
        validateProctoringSettings(
            examRequest.fullscreenRequired ?: false,
            examRequest.allowedPlatforms,
            examRequest.strictMode ?: false,
            examRequest.maxSwitchCount
        )

        val exam = Exam(
            title = examRequest.title,
            description = examRequest.description,
            courseId = courseId,
            creatorId = creatorId,
            startTime = startTime,
            endTime = endTime,
            duration = examRequest.duration,
            totalScore = examRequest.totalScore,
            needsGrading = examRequest.needsGrading ?: false,
            allowedPlatforms = examRequest.allowedPlatforms,
            strictMode = examRequest.strictMode ?: false,
            maxSwitchCount = examRequest.maxSwitchCount,
            fullscreenRequired = examRequest.fullscreenRequired ?: false
        )

        val savedExam = examRepository.save(exam)
        return toExamResponse(savedExam, course.courseName, creator.realName ?: creator.username, 0)
    }

    override fun updateExam(id: Long, examRequest: ExamRequest, userId: Long, userRole: String): ExamResponse {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此考试")
        }

        // Only draft exams can be updated
        if (exam.status != 0) {
            throw IllegalArgumentException("只有草稿状态的考试可以修改")
        }

        val startTime = examRequest.startTime ?: throw IllegalArgumentException("开始时间不能为空")
        val endTime = examRequest.endTime ?: throw IllegalArgumentException("结束时间不能为空")

        // Validate time
        if (endTime <= startTime) {
            throw IllegalArgumentException("结束时间必须晚于开始时间")
        }

        // Validate proctoring settings
        validateProctoringSettings(
            examRequest.fullscreenRequired ?: false,
            examRequest.allowedPlatforms,
            examRequest.strictMode ?: false,
            examRequest.maxSwitchCount
        )

        exam.title = examRequest.title
        exam.description = examRequest.description
        exam.startTime = startTime
        exam.endTime = endTime
        exam.duration = examRequest.duration
        exam.totalScore = examRequest.totalScore
        exam.needsGrading = examRequest.needsGrading ?: false
        exam.allowedPlatforms = examRequest.allowedPlatforms
        exam.strictMode = examRequest.strictMode ?: false
        exam.maxSwitchCount = examRequest.maxSwitchCount
        exam.fullscreenRequired = examRequest.fullscreenRequired ?: false

        val updatedExam = examRepository.save(exam)
        val courseName = resolveCourseName(exam.courseId)
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        val questionCount = examQuestionRepository.countByExamId(id)
        return toExamResponse(updatedExam, courseName, creator.realName ?: creator.username, questionCount)
    }

    override fun deleteExam(id: Long, userId: Long, userRole: String) {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限删除此考试")
        }

        // Delete associated exam questions first
        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(id)
        examQuestionRepository.deleteAll(examQuestions)

        // Delete associated submissions if any
        val submissions = submissionRepository.findByExamId(id)
        if (submissions.isNotEmpty()) {
            submissionRepository.deleteAll(submissions)
        }

        examRepository.delete(exam)
    }

    override fun getExamById(id: Long): ExamResponse {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        val courseName = resolveCourseName(exam.courseId)
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }
        val questionCount = examQuestionRepository.countByExamId(id)
        return toExamResponse(exam, courseName, creator.realName ?: creator.username, questionCount)
    }

    override fun getAllExams(pageable: Pageable): Page<ExamResponse> {
        val examPage = examRepository.findAll(pageable)
        val courseIds = examPage.content.map { it.courseId }.toSet()
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val courses = courseRepository.findAllById(courseIds).associateBy { it.id }
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courses[exam.courseId]?.courseName ?: resolveCourseName(exam.courseId), creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getExamsByStatus(status: Int, pageable: Pageable): Page<ExamResponse> {
        val examPage = examRepository.findByStatus(status, pageable)
        val courseIds = examPage.content.map { it.courseId }.toSet()
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val courses = courseRepository.findAllById(courseIds).associateBy { it.id }
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courses[exam.courseId]?.courseName ?: resolveCourseName(exam.courseId), creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getExamsByCourse(courseId: Long, pageable: Pageable): Page<ExamResponse> {
        val courseName = resolveCourseName(courseId)

        val examPage = examRepository.findByCourseId(courseId, pageable)
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courseName, creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getExamsByCourse(courseId: Long, status: Int?, pageable: Pageable): Page<ExamResponse> {
        val courseName = resolveCourseName(courseId)

        val examPage = if (status != null) {
            examRepository.findByCourseIdAndStatus(courseId, status, pageable)
        } else {
            examRepository.findByCourseId(courseId, pageable)
        }
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courseName, creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getMyExams(userId: Long): List<ExamResponse> {
        val exams = examRepository.findByCreatorId(userId)
        val courseIds = exams.map { it.courseId }.toSet()
        val courses = courseRepository.findAllById(courseIds).associateBy { it.id }
        val creator = userRepository.findById(userId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }
        val examIds = exams.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return exams.map { exam ->
            val courseName = courses[exam.courseId]?.courseName ?: resolveCourseName(exam.courseId)
            toExamResponse(exam, courseName, creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getMyTeachingExams(teacherId: Long, status: Int?, pageable: Pageable): Page<ExamResponse> {
        val courseIds = courseRepository.findByTeacherId(teacherId).map { it.id ?: 0L }
        if (courseIds.isEmpty()) return Page.empty(pageable)

        val examPage = if (status != null) {
            examRepository.findByCourseIdInAndStatus(courseIds, status, pageable)
        } else {
            examRepository.findByCourseIdIn(courseIds, pageable)
        }
        val allCourseIds = examPage.content.map { it.courseId }.toSet()
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val courses = courseRepository.findAllById(allCourseIds).associateBy { it.id }
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courses[exam.courseId]?.courseName ?: resolveCourseName(exam.courseId), creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun searchExams(
        creatorId: Long?,
        status: Int?,
        courseId: Long?,
        pageable: Pageable
    ): Page<ExamResponse> {
        val examPage = examRepository.searchExams(creatorId, status, courseId, pageable)
        val courseIds = examPage.content.map { it.courseId }.toSet()
        val creatorIds = examPage.content.map { it.creatorId }.toSet()
        val courses = courseRepository.findAllById(courseIds).associateBy { it.id }
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = examPage.content.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return examPage.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, courses[exam.courseId]?.courseName ?: resolveCourseName(exam.courseId), creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }
    }

    override fun getStudentAvailableExams(studentId: Long, pageable: Pageable): Page<ExamResponse> {
        // Get student's enrolled courses
        val enrollments = courseSelectionRepository.findByStudentId(studentId)
        if (enrollments.isEmpty()) return Page.empty(pageable)

        val courseIds = enrollments.map { it.courseId }
        val now = LocalDateTime.now()

        // Get all published exams that have not ended. Future exams are visible,
        // but startExam/getExamPaper still block entry before startTime.
        val allExams = courseIds.flatMap { courseId ->
            examRepository.findByCourseIdAndStatus(courseId, 1)
        }.filter { exam ->
            !now.isAfter(exam.endTime)
        }.filter { exam ->
            // Student has not submitted yet
            val submission = submissionRepository.findByExamIdAndUserId(exam.id!!, studentId)
            submission == null || submission.status == 0
        }.sortedWith(compareBy<Exam> { if (now.isBefore(it.startTime)) 1 else 0 }.thenBy { it.startTime })

        // Manual pagination
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(allExams.size)
        val pageContent = if (start < allExams.size) allExams.subList(start, end) else emptyList()

        val creatorIds = pageContent.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val examIds = pageContent.mapNotNull { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(examIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        val responses = pageContent.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(exam, resolveCourseName(exam.courseId), creator.realName ?: creator.username, questionCounts[exam.id] ?: 0)
        }

        return org.springframework.data.domain.PageImpl(responses, pageable, allExams.size.toLong())
    }

    override fun getStudentCompletedExams(studentId: Long, pageable: Pageable): Page<ExamResponse> {
        // Get student's submissions that are submitted or graded
        val submissions = submissionRepository.findByUserId(studentId, Pageable.unpaged()).content
            .filter { it.status >= 1 }

        // Also get exams from enrolled courses that have ended
        val enrollments = courseSelectionRepository.findByStudentId(studentId)
        val courseIds = enrollments.map { it.courseId }
        val now = LocalDateTime.now()

        val endedExams = courseIds.flatMap { courseId ->
            examRepository.findByCourseId(courseId)
        }.filter { exam ->
            exam.status == 2 || now.isAfter(exam.endTime)
        }.filter { exam ->
            // Not already included via submission
            submissions.none { it.examId == exam.id }
        }

        // Combine: exams with submissions + ended exams without submission
        val submittedExamIds = submissions.map { it.examId }.toSet()
        val allExamIds = (submittedExamIds + endedExams.mapNotNull { it.id }).toList()

        if (allExamIds.isEmpty()) return Page.empty(pageable)

        val exams = examRepository.findAllById(allExamIds).sortedByDescending { it.endTime }

        // Manual pagination
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(exams.size)
        val pageContent = if (start < exams.size) exams.subList(start, end) else emptyList()

        val creatorIds = pageContent.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val questionCounts = examQuestionRepository.countByExamIdIn(pageContent.mapNotNull { it.id })
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }
        val scoreByExamId = submissions.associate { it.examId to it.submitScore }

        val responses = pageContent.map { exam ->
            val creator = creators[exam.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toExamResponse(
                exam = exam,
                courseName = resolveCourseName(exam.courseId),
                creatorName = creator.realName ?: creator.username,
                questionCount = questionCounts[exam.id] ?: 0,
                studentScore = scoreByExamId[exam.id]
            )
        }

        return org.springframework.data.domain.PageImpl(responses, pageable, exams.size.toLong())
    }

    override fun patchExam(id: Long, status: Int?, userId: Long, userRole: String): ExamResponse {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此考试")
        }

        // Only support status patch for now
        if (status != null) {
            when (status) {
                1 -> {
                    // Publish
                    if (exam.status != 0) {
                        throw IllegalArgumentException("只有草稿状态的考试可以发布")
                    }
                    val qc = examQuestionRepository.countByExamId(id)
                    if (qc == 0L) {
                        throw IllegalArgumentException("考试至少需要包含一道题目才能发布")
                    }
                    val now = LocalDateTime.now()
                    if (exam.endTime.isBefore(now)) {
                        throw IllegalArgumentException("考试结束时间已过，无法发布")
                    }
                    exam.status = 1
                }
                2 -> {
                    // End exam
                    if (exam.status != 1) {
                        throw IllegalArgumentException("只有已发布的考试可以结束")
                    }
                    exam.status = 2
                }
                else -> throw IllegalArgumentException("不支持的状态值: $status")
            }
        }

        val patchedExam = examRepository.save(exam)

        // Auto-send notification when exam is published
        if (status == 1 && exam.status == 1) {
            val studentIds = courseSelectionRepository.findByCourseId(exam.courseId)
                .map { it.studentId }
            if (studentIds.isNotEmpty()) {
                notificationService.sendExamPublishedNotification(
                    examId = id,
                    examTitle = exam.title,
                    studentIds = studentIds
                )
            }
        }

        val courseName = resolveCourseName(exam.courseId)
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }
        val finalQuestionCount = examQuestionRepository.countByExamId(id)
        return toExamResponse(patchedExam, courseName, creator.realName ?: creator.username, finalQuestionCount)
    }

    override fun publishExam(id: Long, userId: Long, userRole: String): ExamResponse {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限发布此考试")
        }

        // Only draft exams can be published
        if (exam.status != 0) {
            throw IllegalArgumentException("只有草稿状态的考试可以发布")
        }

        // Validate has questions
        val questionCount = examQuestionRepository.countByExamId(id)
        if (questionCount == 0L) {
            throw IllegalArgumentException("考试至少需要包含一道题目才能发布")
        }

        // Validate time is valid
        val now = LocalDateTime.now()
        if (exam.endTime.isBefore(now)) {
            throw IllegalArgumentException("考试结束时间已过，无法发布")
        }

        exam.status = 1
        val publishedExam = examRepository.save(exam)

        val courseName = resolveCourseName(exam.courseId)
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }
        return toExamResponse(publishedExam, courseName, creator.realName ?: creator.username, questionCount)
    }

    override fun addQuestionToExam(examId: Long, request: ExamQuestionRequest, userId: Long, userRole: String) {
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此考试")
        }

        // Only draft exams can add questions
        if (exam.status != 0) {
            throw IllegalArgumentException("只有草稿状态的考试可以添加题目")
        }

        val questionId = request.questionId ?: throw IllegalArgumentException("题目ID不能为空")

        // Validate question exists
        questionRepository.findById(questionId).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Check if already exists
        if (examQuestionRepository.existsByExamIdAndQuestionId(examId, questionId)) {
            throw IllegalArgumentException("该题目已在考试中")
        }

        val examQuestion = ExamQuestion(
            examId = examId,
            questionId = questionId,
            score = request.score!!,
            sequence = request.sequence!!
        )

       examQuestionRepository.save(examQuestion)
    }

    override fun removeQuestionFromExam(examId: Long, questionId: Long, userId: Long, userRole: String) {
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此考试")
        }

        // Only draft exams can remove questions
        if (exam.status != 0) {
            throw IllegalArgumentException("只有草稿状态的考试可以移除题目")
        }

        // Check if exists
        if (!examQuestionRepository.existsByExamIdAndQuestionId(examId, questionId)) {
            throw IllegalArgumentException("该题目不在考试中")
        }

        examQuestionRepository.deleteByExamIdAndQuestionId(examId, questionId)
    }

    override fun getExamQuestions(examId: Long): List<ExamQuestionResponse> {
        // Verify exam exists
        examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(examId)
        val questionIds = examQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

        return examQuestions.mapNotNull { eq ->
            questions[eq.questionId]?.let {
                ExamQuestionResponse(
                    examId = eq.examId,
                    questionId = eq.questionId,
                    questionContent = it.content,
                    questionType = it.type,
                    questionDifficulty = it.difficulty,
                    score = eq.score,
                    sequence = eq.sequence
                )
            }
        }
    }

    override fun getExamPaper(examId: Long, studentId: Long): List<ExamPaperQuestionResponse> {
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Must be published
        if (exam.status != 1) {
            throw IllegalArgumentException("考试未发布或已结束")
        }

        // Must be within exam time
        val now = LocalDateTime.now()
        if (now.isBefore(exam.startTime)) {
            throw IllegalArgumentException("考试尚未开始")
        }
        if (now.isAfter(exam.endTime)) {
            throw IllegalArgumentException("考试已结束")
        }

        // Must be enrolled in the course
        val isEnrolled = courseSelectionRepository.existsByStudentIdAndCourseId(studentId, exam.courseId)
        if (!isEnrolled) {
            throw IllegalArgumentException("您未选修该课程，无法参加考试")
        }

        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(examId)
        val questionIds = examQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

        return examQuestions.mapNotNull { eq ->
            questions[eq.questionId]?.let { q ->
                ExamPaperQuestionResponse(
                    examId = eq.examId,
                    questionId = eq.questionId,
                    questionContent = q.content,
                    questionType = q.type,
                    questionDifficulty = q.difficulty,
                    options = q.options,
                    score = eq.score,
                    sequence = eq.sequence
                )
            }
        }
    }

    override fun composeRandomExam(
        examId: Long,
        request: ComposeRandomExamRequest,
        userId: Long,
        userRole: String
    ): ExamResponse {
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Permission check
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此考试")
        }

        // Only draft exams can be composed
        if (exam.status != 0) {
            throw IllegalArgumentException("只有草稿状态的考试可以组卷")
        }

        // Validate bank exists
        val bankId = request.bankId!!
        if (!questionBankQuestionRepository.findByBankId(bankId).any()) {
            throw IllegalArgumentException("题库不存在或题库为空")
        }

        val validTypes = setOf("single", "multiple", "true_false", "fill_blank", "short_answer")
        val validDifficulties = setOf("easy", "medium", "hard")
        val lenientMode = request.options?.lenientMode ?: false

        // Fetch all questions in the bank
        val bankQuestions = questionBankQuestionRepository.findByBankId(bankId)
        val questionIds = bankQuestions.map { it.questionId }
        val allQuestions = questionRepository.findAllById(questionIds)

        // Build candidate pools: type -> difficulty -> list of questions
        val candidatePool: Map<String, Map<String, List<ovo.sypw.onlineexamsystemback.entity.Question>>> = allQuestions
            .groupBy { it.type }
            .mapValues { (_, questions) ->
                questions.groupBy { it.difficulty }
            }

        // Collect selected exam questions
        val selectedExamQuestions = mutableListOf<ExamQuestion>()
        val selectedQuestionIds = mutableSetOf<Long>()

        for ((sectionIndex, section) in request.sections.withIndex()) {
            // Validate type
            if (section.type !in validTypes) {
                throw IllegalArgumentException("题型 '${section.type}' 无效")
            }

            // Validate count and score
            if (section.count <= 0) {
                throw IllegalArgumentException("题目数量必须大于0")
            }
            if (section.scorePerQuestion <= 0) {
                throw IllegalArgumentException("每题分值必须大于0")
            }

            // Validate difficulty distribution if provided
            val distribution = section.difficultyDistribution
            if (distribution != null) {
                val total = distribution.values.sum()
                if (total != section.count) {
                    throw IllegalArgumentException(
                        "题型 '${section.type}' 的难度分配之和($total) 不等于题目数量(${section.count})"
                    )
                }
                for ((diff, _) in distribution) {
                    if (diff !in validDifficulties) {
                        throw IllegalArgumentException("难度 '$diff' 无效")
                    }
                }
            }

            // Pick questions
            val typeCandidates = candidatePool[section.type]
                ?: throw IllegalArgumentException("题库中不存在 '${section.type}' 类型的题目")

            val pickedQuestions = mutableListOf<ovo.sypw.onlineexamsystemback.entity.Question>()

            if (distribution != null) {
                // Strict difficulty distribution
                val deficits = mutableListOf<Pair<String, Int>>() // (difficulty, shortage)

                for ((difficulty, needed) in distribution) {
                    val pool = typeCandidates[difficulty]?.filter { q -> (q.id ?: 0L) !in selectedQuestionIds }
                        ?: emptyList()
                    val shuffled = pool.shuffled()
                    if (shuffled.size < needed) {
                        if (!lenientMode) {
                            throw IllegalArgumentException(
                                "题库中 '${section.type}' 类型的 '$difficulty' 难度题目不足，需要 $needed 道，实际只有 ${shuffled.size} 道"
                            )
                        }
                        deficits.add(difficulty to (needed - shuffled.size))
                        pickedQuestions.addAll(shuffled)
                    } else {
                        pickedQuestions.addAll(shuffled.take(needed))
                    }
                }

                // Lenient mode: fill deficits from other difficulties of the same type
                if (lenientMode && deficits.isNotEmpty()) {
                    val totalDeficit = deficits.sumOf { it.second }
                    val pickedIds = pickedQuestions.mapNotNull { it.id }.toSet()
                    val otherPool = typeCandidates.flatMap { (diff, questions) ->
                        if (distribution.containsKey(diff)) emptyList()
                        else questions.filter { q -> (q.id ?: 0L) !in selectedQuestionIds && (q.id ?: 0L) !in pickedIds }
                    }.shuffled()
                    if (otherPool.size < totalDeficit) {
                        throw IllegalArgumentException(
                            "题库中 '${section.type}' 类型题目总数不足，需要 ${section.count} 道，实际只有 ${pickedQuestions.size + otherPool.size} 道"
                        )
                    }
                    pickedQuestions.addAll(otherPool.take(totalDeficit))
                }
            } else {
                // No difficulty distribution: random from all difficulties of this type
                val pool = typeCandidates.values.flatten().filter { q -> (q.id ?: 0L) !in selectedQuestionIds }.shuffled()
                if (pool.size < section.count) {
                    throw IllegalArgumentException(
                        "题库中 '${section.type}' 类型题目不足，需要 ${section.count} 道，实际只有 ${pool.size} 道"
                    )
                }
                pickedQuestions.addAll(pool.take(section.count))
            }

            // Add to selected list
            for ((index, question) in pickedQuestions.withIndex()) {
                val qId = question.id ?: throw IllegalStateException("题目ID不能为空")
                selectedQuestionIds.add(qId)
                selectedExamQuestions.add(
                    ExamQuestion(
                        examId = examId,
                        questionId = qId,
                        score = section.scorePerQuestion,
                        sequence = sectionIndex * 1000 + index + 1
                    )
                )
            }
        }

        // Shuffle questions if requested
        val finalQuestions = if (request.options?.shuffleQuestions != false) {
            selectedExamQuestions.shuffled().mapIndexed { index, eq ->
                ExamQuestion(
                    examId = eq.examId,
                    questionId = eq.questionId,
                    score = eq.score,
                    sequence = index + 1
                )
            }
        } else {
            selectedExamQuestions.sortedBy { it.sequence }
        }

        // Clear existing questions and save new ones (overwrite mode)
        examQuestionRepository.deleteByExamId(examId)
        examQuestionRepository.saveAll(finalQuestions)

        // Update total score
        val totalScore = request.sections.sumOf { it.count * it.scorePerQuestion }
        exam.totalScore = totalScore

        // Validate expected total score
        if (request.expectedTotalScore != null && request.expectedTotalScore != totalScore) {
            throw IllegalArgumentException(
                "期望总分 ${request.expectedTotalScore} 分，实际组卷总分 ${totalScore} 分，请检查各题型数量和分值配置"
            )
        }

        val savedExam = examRepository.save(exam)

        val courseName = resolveCourseName(exam.courseId)
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }
        val questionCount = examQuestionRepository.countByExamId(examId)
        return toExamResponse(savedExam, courseName, creator.realName ?: creator.username, questionCount)
    }

    override fun batchDelete(ids: List<Long>, userId: Long, userRole: String): BatchDeleteResult {
        val successIds = mutableListOf<Long>()
        val failedDetails = mutableListOf<FailedDetail>()

        for (id in ids.distinct()) {
            try {
                deleteExam(id, userId, userRole)
                successIds.add(id)
            } catch (e: IllegalArgumentException) {
                failedDetails.add(FailedDetail(id, e.message ?: "删除失败"))
            } catch (e: Exception) {
                failedDetails.add(FailedDetail(id, "系统错误: ${e.message}"))
            }
        }

        return BatchDeleteResult(
            successCount = successIds.size,
            failedCount = failedDetails.size,
            successIds = successIds,
            failedDetails = failedDetails
        )
    }

    private fun validateProctoringSettings(
        fullscreenRequired: Boolean,
        allowedPlatforms: String?,
        strictMode: Boolean,
        maxSwitchCount: Int?
    ) {
        // If fullscreen required, must allow desktop
        if (fullscreenRequired && allowedPlatforms == "mobile") {
            throw IllegalArgumentException("要求全屏时不能仅允许移动端")
        }

        // If strict mode with max switch count, must be positive
        if (strictMode && maxSwitchCount != null && maxSwitchCount <= 0) {
            throw IllegalArgumentException("最大切出次数必须大于0")
        }
    }

    private fun resolveCourseName(courseId: Long): String {
        return courseRepository.findById(courseId).orElse(null)?.courseName ?: "课程已删除"
    }

    private fun toExamResponse(
        exam: Exam,
        courseName: String,
        creatorName: String,
        questionCount: Long,
        studentScore: Int? = null
    ): ExamResponse {
        val statusDescription = when {
            exam.status == 1 && LocalDateTime.now().isBefore(exam.startTime) -> "考试时间未到"
            else -> when (exam.status) {
                0 -> "草稿"
                1 -> "已发布"
                2 -> "已结束"
                else -> "未知"
            }
        }

        return ExamResponse(
            id = exam.id ?: 0L,
            title = exam.title,
            description = exam.description,
            courseId = exam.courseId,
            courseName = courseName,
            creatorId = exam.creatorId,
            creatorName = creatorName,
            startTime = exam.startTime,
            endTime = exam.endTime,
            duration = exam.duration,
            totalScore = exam.totalScore,
            studentScore = studentScore,
            status = exam.status,
            statusDescription = statusDescription,
            needsGrading = exam.needsGrading,
            questionCount = questionCount,
            allowedPlatforms = exam.allowedPlatforms,
            strictMode = exam.strictMode,
            maxSwitchCount = exam.maxSwitchCount,
            fullscreenRequired = exam.fullscreenRequired,
            createTime = exam.createTime
        )
    }
}
