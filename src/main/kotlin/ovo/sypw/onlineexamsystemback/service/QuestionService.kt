package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.QuestionRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse

interface QuestionService {
    // CRUD operations
    fun createQuestion(questionRequest: QuestionRequest, creatorId: Long): QuestionResponse
    fun updateQuestion(id: Long, questionRequest: QuestionRequest, userId: Long, userRole: String): QuestionResponse
    fun deleteQuestion(id: Long, userId: Long, userRole: String)
    fun getQuestionById(id: Long): QuestionResponse
    fun getAllQuestions(): List<QuestionResponse>
    fun getMyQuestions(userId: Long): List<QuestionResponse>
    
    // Filter operations
    fun getQuestionsByType(type: String): List<QuestionResponse>
    fun getQuestionsByDifficulty(difficulty: String): List<QuestionResponse>
    fun getQuestionsByCategory(category: String): List<QuestionResponse>
}
