package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.ExamSubmission
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExamSubmissionRepository : JpaRepository<ExamSubmission, Long> {
    // Find all submissions for an exam
    fun findByExamId(examId: Long): List<ExamSubmission>
    fun findByExamId(examId: Long, pageable: Pageable): Page<ExamSubmission>

    // Find all submissions by a user
    fun findByUserId(userId: Long): List<ExamSubmission>
    fun findByUserId(userId: Long, pageable: Pageable): Page<ExamSubmission>

    // Find specific submission
    fun findByExamIdAndUserId(examId: Long, userId: Long): ExamSubmission?

    // Check if user has submitted
    fun existsByExamIdAndUserId(examId: Long, userId: Long): Boolean

    // Filter by status
    fun findByStatus(status: Int): List<ExamSubmission>

    // Find submissions by exam and status
    fun findByExamIdAndStatus(examId: Long, status: Int): List<ExamSubmission>

    // Find submissions by multiple exam IDs (batch query)
    fun findByExamIdIn(examIds: List<Long>): List<ExamSubmission>

    // Count submissions for an exam
    fun countByExamId(examId: Long): Long
}
