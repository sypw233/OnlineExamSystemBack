package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.QuestionBankRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionBankResponse
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuestionBankService {
    // CRUD operations
    fun createQuestionBank(request: QuestionBankRequest, creatorId: Long): QuestionBankResponse
    fun updateQuestionBank(id: Long, request: QuestionBankRequest, userId: Long, userRole: String): QuestionBankResponse
    fun deleteQuestionBank(id: Long, userId: Long, userRole: String)
    fun getQuestionBankById(id: Long): QuestionBankResponse
    fun getAllQuestionBanks(pageable: Pageable): Page<QuestionBankResponse>
    fun getMyQuestionBanks(userId: Long, pageable: Pageable): Page<QuestionBankResponse>
    
    // Question-Bank association
    fun addQuestionToBank(bankId: Long, questionId: Long, userId: Long, userRole: String)
    fun removeQuestionFromBank(bankId: Long, questionId: Long, userId: Long, userRole: String)
    fun getQuestionsInBank(bankId: Long): List<QuestionResponse>
}
