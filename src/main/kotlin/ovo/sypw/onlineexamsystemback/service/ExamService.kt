package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ExamService {
    // Exam CRUD
    fun createExam(examRequest: ExamRequest, creatorId: Long): ExamResponse
    fun updateExam(id: Long, examRequest: ExamRequest, userId: Long, userRole: String): ExamResponse
    fun patchExam(id: Long, status: Int?, userId: Long, userRole: String): ExamResponse
    fun deleteExam(id: Long, userId: Long, userRole: String)
    fun getExamById(id: Long): ExamResponse
    fun getAllExams(pageable: Pageable): Page<ExamResponse>
    fun getExamsByStatus(status: Int, pageable: Pageable): Page<ExamResponse>
    fun getExamsByCourse(courseId: Long, pageable: Pageable): Page<ExamResponse>
    fun getMyExams(userId: Long): List<ExamResponse>
    fun getMyTeachingExams(teacherId: Long, pageable: Pageable): Page<ExamResponse>

    // Unified search
    fun searchExams(
        creatorId: Long?,
        status: Int?,
        courseId: Long?,
        pageable: Pageable
    ): Page<ExamResponse>

    // Publish management (kept for internal use)
    fun publishExam(id: Long, userId: Long, userRole: String): ExamResponse

    // Question management
    fun addQuestionToExam(examId: Long, request: ExamQuestionRequest, userId: Long, userRole: String)
    fun removeQuestionFromExam(examId: Long, questionId: Long, userId: Long, userRole: String)
    fun getExamQuestions(examId: Long): List<ExamQuestionResponse>
}
