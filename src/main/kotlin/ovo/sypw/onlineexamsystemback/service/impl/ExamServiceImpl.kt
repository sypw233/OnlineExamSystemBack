package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse
import ovo.sypw.onlineexamsystemback.entity.Exam
import ovo.sypw.onlineexamsystemback.entity.ExamQuestion
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.ExamService
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
    private val questionRepository: QuestionRepository
) : ExamService {

    override fun createExam(examRequest: ExamRequest, creatorId: Long): ExamResponse {
        val creator = userRepository.findById(creatorId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        if (creator.role != "teacher" && creator.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以创建考试")
        }

        // Validate course exists
        val course = courseRepository.findById(examRequest.courseId!!).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Validate time
        if (examRequest.endTime!! <= examRequest.startTime!!) {
            throw IllegalArgumentException("结束时间必须晚于开始时间")
        }

        // Validate proctoring settings
        validateProctoringSettings(
            examRequest.fullscreenRequired,
            examRequest.allowedPlatforms,
            examRequest.strictMode,
            examRequest.maxSwitchCount
        )

        val exam = Exam(
            title = examRequest.title,
            description = examRequest.description,
            courseId = examRequest.courseId,
            creatorId = creatorId,
            startTime = examRequest.startTime,
            endTime = examRequest.endTime,
            duration = examRequest.duration,
            totalScore = examRequest.totalScore,
            needsGrading = examRequest.needsGrading,
            allowedPlatforms = examRequest.allowedPlatforms,
            strictMode = examRequest.strictMode,
            maxSwitchCount = examRequest.maxSwitchCount,
            fullscreenRequired = examRequest.fullscreenRequired
        )

        val savedExam = examRepository.save(exam)
        return toExamResponse(savedExam, course.courseName, creator.realName ?: creator.username)
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

        // Validate time
        if (examRequest.endTime!! <= examRequest.startTime!!) {
            throw IllegalArgumentException("结束时间必须晚于开始时间")
        }

        // Validate proctoring settings
        validateProctoringSettings(
            examRequest.fullscreenRequired,
            examRequest.allowedPlatforms,
            examRequest.strictMode,
            examRequest.maxSwitchCount
        )

        exam.title = examRequest.title
        exam.description = examRequest.description
        exam.startTime = examRequest.startTime
        exam.endTime = examRequest.endTime
        exam.duration = examRequest.duration
        exam.totalScore = examRequest.totalScore
        exam.needsGrading = examRequest.needsGrading
        exam.allowedPlatforms = examRequest.allowedPlatforms
        exam.strictMode = examRequest.strictMode
        exam.maxSwitchCount = examRequest.maxSwitchCount
        exam.fullscreenRequired = examRequest.fullscreenRequired

        val updatedExam = examRepository.save(exam)
        val course = courseRepository.findById(exam.courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toExamResponse(updatedExam, course.courseName, creator.realName ?: creator.username)
    }

    override fun deleteExam(id: Long, userId: Long, userRole: String) {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Check permission
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限删除此考试")
        }

        // TODO: Check if exam has submissions (will be implemented in submission module)
        
        examRepository.delete(exam)
    }

    override fun getExamById(id: Long): ExamResponse {
        val exam = examRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        val course = courseRepository.findById(exam.courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toExamResponse(exam, course.courseName, creator.realName ?: creator.username)
    }

    override fun getAllExams(): List<ExamResponse> {
        val exams = examRepository.findAll()
        return exams.map { exam ->
            val course = courseRepository.findById(exam.courseId).orElseThrow {
                throw IllegalArgumentException("课程不存在")
            }
            val creator = userRepository.findById(exam.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toExamResponse(exam, course.courseName, creator.realName ?: creator.username)
        }
    }

    override fun getExamsByStatus(status: Int): List<ExamResponse> {
        val exams = examRepository.findByStatus(status)
        return exams.map { exam ->
            val course = courseRepository.findById(exam.courseId).orElseThrow {
                throw IllegalArgumentException("课程不存在")
            }
            val creator = userRepository.findById(exam.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toExamResponse(exam, course.courseName, creator.realName ?: creator.username)
        }
    }

    override fun getExamsByCourse(courseId: Long): List<ExamResponse> {
        val exams = examRepository.findByCourseId(courseId)
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        return exams.map { exam ->
            val creator = userRepository.findById(exam.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toExamResponse(exam, course.courseName, creator.realName ?: creator.username)
        }
    }

    override fun getMyExams(userId: Long): List<ExamResponse> {
        val exams = examRepository.findByCreatorId(userId)
        return exams.map { exam ->
            val course = courseRepository.findById(exam.courseId).orElseThrow {
                throw IllegalArgumentException("课程不存在")
            }
            val creator = userRepository.findById(userId).orElseThrow {
                throw IllegalArgumentException("用户不存在")
            }
            toExamResponse(exam, course.courseName, creator.realName ?: creator.username)
        }
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

        val course = courseRepository.findById(exam.courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }
        val creator = userRepository.findById(exam.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toExamResponse(publishedExam, course.courseName, creator.realName ?: creator.username)
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

        // Validate question exists
        questionRepository.findById(request.questionId!!).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Check if already exists
        if (examQuestionRepository.existsByExamIdAndQuestionId(examId, request.questionId)) {
            throw IllegalArgumentException("该题目已在考试中")
        }

        val examQuestion = ExamQuestion(
            examId = examId,
            questionId = request.questionId,
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
        return examQuestions.mapNotNull { eq ->
            val question = questionRepository.findById(eq.questionId).orElse(null)
            question?.let {
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

    private fun toExamResponse(exam: Exam, courseName: String, creatorName: String): ExamResponse {
        val questionCount = examQuestionRepository.countByExamId(exam.id ?: 0L)
        val statusDescription = when (exam.status) {
            0 -> "草稿"
            1 -> "已发布"
            2 -> "已结束"
            else -> "未知"
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
