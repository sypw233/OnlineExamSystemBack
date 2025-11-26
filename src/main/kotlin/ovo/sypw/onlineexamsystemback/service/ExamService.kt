package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse

interface ExamService {
    // Exam CRUD
    fun createExam(examRequest: ExamRequest, creatorId: Long): ExamResponse
    fun updateExam(id: Long, examRequest: ExamRequest, userId: Long, userRole: String): ExamResponse
    fun deleteExam(id: Long, userId: Long, userRole: String)
    fun getExamById(id: Long): ExamResponse
    fun getAllExams(): List<ExamResponse>
    fun getExamsByStatus(status: Int): List<ExamResponse>
    fun getExamsByCourse(courseId: Long): List<ExamResponse>
    fun getMyExams(userId: Long): List<ExamResponse>
    
    // Publish management
    fun publishExam(id: Long, userId: Long, userRole: String): ExamResponse
    
    // Question management
    fun addQuestionToExam(examId: Long, request: ExamQuestionRequest, userId: Long, userRole: String)
    fun removeQuestionFromExam(examId: Long, questionId: Long, userId: Long, userRole: String)
    fun getExamQuestions(examId: Long): List<ExamQuestionResponse>
}
