package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.QuestionBankQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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

    // Batch count questions in multiple banks
    @Query("SELECT qbq.bankId, COUNT(qbq) FROM QuestionBankQuestion qbq WHERE qbq.bankId IN :bankIds GROUP BY qbq.bankId")
    fun countByBankIdIn(@org.springframework.data.repository.query.Param("bankIds") bankIds: List<Long>): List<Array<Any>>

    // Count banks containing a question
    fun countByQuestionId(questionId: Long): Long

    // Batch count banks containing multiple questions
    @Query("SELECT qbq.questionId, COUNT(qbq) FROM QuestionBankQuestion qbq WHERE qbq.questionId IN :questionIds GROUP BY qbq.questionId")
    fun countByQuestionIdIn(@org.springframework.data.repository.query.Param("questionIds") questionIds: List<Long>): List<Array<Any>>
}
