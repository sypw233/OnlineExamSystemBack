package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import ovo.sypw.onlineexamsystemback.dto.response.*
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.StatisticsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.round

@Service
@Transactional(readOnly = true)
class StatisticsServiceImpl(
    private val examRepository: ExamRepository,
    private val submissionRepository: ExamSubmissionRepository,
    private val examQuestionRepository: ExamQuestionRepository,
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val objectMapper: ObjectMapper
) : StatisticsService {

    companion object {
        private const val PASSING_SCORE = 60  // 及格分数线
    }

    override fun getExamStatistics(examId: Long): ExamStatisticsResponse {
        // Validate exam exists
        val exam = examRepository.findById(examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // Get all submissions for this exam
        val submissions = submissionRepository.findByExamId(examId)
        val submittedSubmissions = submissions.filter { it.status >= 1 }  // Only submitted

        // Calculate statistics
        val scores = submittedSubmissions.mapNotNull { it.submitScore }
        val totalStudents = submissions.distinctBy { it.userId }.size
        val submittedCount = submittedSubmissions.size
        val completionRate = if (totalStudents > 0) {
            round(submittedCount.toDouble() / totalStudents * 100 * 10) / 10
        } else 0.0

        val averageScore = if (scores.isNotEmpty()) {
            round(scores.average() * 10) / 10
        } else null

        val highestScore = scores.maxOrNull()
        val lowestScore = scores.minOrNull()

        val passCount = scores.count { it >= PASSING_SCORE }
        val passRate = if (scores.isNotEmpty()) {
            round(passCount.toDouble() / scores.size * 100 * 10) / 10
        } else 0.0

        // Score distribution
        val scoreDistribution = mutableMapOf(
            "0-59" to 0,
            "60-69" to 0,
            "70-79" to 0,
            "80-89" to 0,
            "90-100" to 0
        )

        scores.forEach { score ->
            when {
                score < 60 -> scoreDistribution["0-59"] = scoreDistribution["0-59"]!! + 1
                score < 70 -> scoreDistribution["60-69"] = scoreDistribution["60-69"]!! + 1
                score < 80 -> scoreDistribution["70-79"] = scoreDistribution["70-79"]!! + 1
                score < 90 -> scoreDistribution["80-89"] = scoreDistribution["80-89"]!! + 1
                else -> scoreDistribution["90-100"] = scoreDistribution["90-100"]!! + 1
            }
        }

        return ExamStatisticsResponse(
            examId = examId,
            examTitle = exam.title,
            totalStudents = totalStudents,
            submittedCount = submittedCount,
            completionRate = completionRate,
            averageScore = averageScore,
            highestScore = highestScore,
            lowestScore = lowestScore,
            passCount = passCount,
            passRate = passRate,
            scoreDistribution = scoreDistribution
        )
    }

    override fun getCourseStatistics(courseId: Long): CourseStatisticsResponse {
        // Validate course exists
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Get all exams for this course
        val exams = examRepository.findByCourseId(courseId)
        val examIds = exams.map { it.id!! }

        // Get all submissions for these exams
        val allSubmissions = examIds.flatMap { examId ->
            submissionRepository.findByExamId(examId).filter { it.status >= 1 }
        }

        // Calculate statistics
        val studentIds = allSubmissions.map { it.userId }.distinct()
        val scores = allSubmissions.mapNotNull { it.submitScore }

        val averageScore = if (scores.isNotEmpty()) {
            round(scores.average() * 10) / 10
        } else null

        val highestScore = scores.maxOrNull()
        val lowestScore = scores.minOrNull()

        return CourseStatisticsResponse(
            courseId = courseId,
            courseName = course.courseName,
            totalStudents = studentIds.size,
            totalExams = exams.size,
            averageScore = averageScore,
            highestScore = highestScore,
            lowestScore = lowestScore
        )
    }

    override fun getQuestionStatistics(questionId: Long): QuestionStatisticsResponse {
        // Validate question exists
        val question = questionRepository.findById(questionId).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Get all exams that use this question
        val examQuestions = examQuestionRepository.findByQuestionId(questionId)
        val usageCount = examQuestions.size

        // Get all submissions for exams using this question
        var totalAttempts = 0
        var correctCount = 0
        val optionCounts = mutableMapOf<String, Int>()

        examQuestions.forEach { eq ->
            val submissions = submissionRepository.findByExamId(eq.examId).filter { it.status >= 1 }

            submissions.forEach { submission ->
                if (submission.answers != null) {
                    try {
                        val answers = objectMapper.readValue(
                            submission.answers!!, 
                            object : TypeReference<Map<String, String>>() {}
                        )

                        val userAnswer = answers[questionId.toString()]
                        if (userAnswer != null) {
                            totalAttempts++

                            // Check if correct
                            val correctAnswer = question.answer?.trim() ?: ""
                            val isCorrect = when (question.type) {
                                "single", "true_false" -> {
                                    userAnswer.trim().equals(correctAnswer, ignoreCase = true)
                                }
                                "multiple" -> {
                                    val userOptions = userAnswer.split(",").map { it.trim() }.toSet()
                                    val correctOptions = correctAnswer.split(",").map { it.trim() }.toSet()
                                    userOptions == correctOptions
                                }
                                else -> false  // fill_blank, short_answer need manual grading
                            }

                            if (isCorrect) {
                                correctCount++
                            }

                            // Count option distribution for objective questions
                            if (question.type in listOf("single", "multiple", "true_false")) {
                                val options = userAnswer.split(",").map { it.trim() }
                                options.forEach { option ->
                                    optionCounts[option] = optionCounts.getOrDefault(option, 0) + 1
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip invalid answer data
                    }
                }
            }
        }

        val accuracy = if (totalAttempts > 0) {
            round(correctCount.toDouble() / totalAttempts * 100 * 10) / 10
        } else 0.0

        val optionDistribution = if (question.type in listOf("single", "multiple", "true_false")) {
            optionCounts
        } else null

        return QuestionStatisticsResponse(
            questionId = questionId,
            questionContent = question.content,
            questionType = question.type,
            usageCount = usageCount,
            totalAttempts = totalAttempts,
            correctCount = correctCount,
            accuracy = accuracy,
            optionDistribution = optionDistribution
        )
    }

    override fun getStudentStatistics(studentId: Long): StudentStatisticsResponse {
        // Validate student exists
        val student = userRepository.findById(studentId).orElseThrow {
            throw IllegalArgumentException("学生不存在")
        }

        // Get all submissions for this student
        val submissions = submissionRepository.findByUserId(studentId).filter { it.status >= 1 }

        val scores = submissions.mapNotNull { it.submitScore }
        val averageScore = if (scores.isNotEmpty()) {
            round(scores.average() * 10) / 10
        } else null

        val highestScore = scores.maxOrNull()
        val lowestScore = scores.minOrNull()

        // Get score records
        val scoreRecords = submissions.mapNotNull { submission ->
            val exam = examRepository.findById(submission.examId).orElse(null)
            if (exam != null) {
                StudentScoreRecord(
                    examId = submission.examId,
                    examTitle = exam.title,
                    score = submission.submitScore,
                    submitTime = submission.submitTime
                )
            } else {
                null
            }
        }.sortedByDescending { it.submitTime }

        return StudentStatisticsResponse(
            studentId = studentId,
            studentName = student.realName ?: student.username,
            totalExams = submissions.size,
            averageScore = averageScore,
            highestScore = highestScore,
            lowestScore = lowestScore,
            scores = scoreRecords
        )
    }

    override fun getSystemOverview(): SystemOverviewResponse {
        val allUsers = userRepository.findAll()
        val studentCount = allUsers.count { it.role == "student" }
        val teacherCount = allUsers.count { it.role == "teacher" }
        val adminCount = allUsers.count { it.role == "admin" }

        val totalCourses = courseRepository.count().toInt()
        val totalExams = examRepository.count().toInt()
        val totalQuestions = questionRepository.count().toInt()
        val totalSubmissions = submissionRepository.count().toInt()

        return SystemOverviewResponse(
            totalUsers = allUsers.size,
            studentCount = studentCount,
            teacherCount = teacherCount,
            adminCount = adminCount,
            totalCourses = totalCourses,
            totalExams = totalExams,
            totalQuestions = totalQuestions,
            totalSubmissions = totalSubmissions
        )
    }
}
