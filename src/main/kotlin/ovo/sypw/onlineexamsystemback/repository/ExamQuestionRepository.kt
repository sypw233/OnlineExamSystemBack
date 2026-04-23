package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.ExamQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ExamQuestionRepository : JpaRepository<ExamQuestion, Long> {
    // Find all questions in an exam, ordered by sequence
    fun findByExamIdOrderBySequence(examId: Long): List<ExamQuestion>
    
    // Check if question is already in exam
    fun existsByExamIdAndQuestionId(examId: Long, questionId: Long): Boolean
    
    // Remove question from exam
    @Transactional
    fun deleteByExamIdAndQuestionId(examId: Long, questionId: Long)

    // Remove all questions from exam (for random compose overwrite)
    @Transactional
    fun deleteByExamId(examId: Long)

    // Count questions in exam
    fun countByExamId(examId: Long): Long

    // Batch count questions in multiple exams
    @Query("SELECT eq.examId, COUNT(eq) FROM ExamQuestion eq WHERE eq.examId IN :examIds GROUP BY eq.examId")
    fun countByExamIdIn(@Param("examIds") examIds: List<Long>): List<Array<Any>>

    // Find by exam and question
    fun findByExamIdAndQuestionId(examId: Long, questionId: Long): ExamQuestion?

    // Find all exams using a question (for statistics)
    fun findByQuestionId(questionId: Long): List<ExamQuestion>
}
