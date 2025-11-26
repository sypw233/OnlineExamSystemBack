package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.QuestionBankQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface QuestionBankQuestionRepository : JpaRepository<QuestionBankQuestion, Long> {
    // Find all questions in a bank
    fun findByBankId(bankId: Long): List<QuestionBankQuestion>
    
    // Find all banks containing a question
    fun findByQuestionId(questionId: Long): List<QuestionBankQuestion>
    
    // Check if question is in bank
    fun existsByBankIdAndQuestionId(bankId: Long, questionId: Long): Boolean
    
    // Remove question from bank
    @Transactional
    fun deleteByBankIdAndQuestionId(bankId: Long, questionId: Long)
    
    // Count questions in a bank
    fun countByBankId(bankId: Long): Long
    
    // Count banks containing a question
    fun countByQuestionId(questionId: Long): Long
}
