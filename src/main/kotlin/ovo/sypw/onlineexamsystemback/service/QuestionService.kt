package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.QuestionRequest
import ovo.sypw.onlineexamsystemback.dto.response.BatchDeleteResult
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuestionService {
    // CRUD operations
    fun createQuestion(questionRequest: QuestionRequest, creatorId: Long): QuestionResponse
    fun updateQuestion(id: Long, questionRequest: QuestionRequest, userId: Long, userRole: String): QuestionResponse
    fun deleteQuestion(id: Long, userId: Long, userRole: String)
    fun getQuestionById(id: Long): QuestionResponse
    fun getAllQuestions(pageable: Pageable): Page<QuestionResponse>
    fun getMyQuestions(userId: Long, pageable: Pageable): Page<QuestionResponse>

    // Filter operations (paginated)
    fun getQuestionsByType(type: String, pageable: Pageable): Page<QuestionResponse>
    fun getQuestionsByDifficulty(difficulty: String, pageable: Pageable): Page<QuestionResponse>
    fun getQuestionsByCategory(category: String, pageable: Pageable): Page<QuestionResponse>

    // Unified search with optional filters
    fun searchQuestions(
        creatorId: Long?,
        type: String?,
        difficulty: String?,
        category: String?,
        pageable: Pageable
    ): Page<QuestionResponse>

    // Batch delete
    fun batchDelete(ids: List<Long>, userId: Long, userRole: String): BatchDeleteResult
}
