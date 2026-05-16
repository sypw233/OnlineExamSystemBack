package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Exam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ExamRepository : JpaRepository<Exam, Long> {
    // Find exams by course
    fun findByCourseId(courseId: Long): List<Exam>
    fun findByCourseId(courseId: Long, pageable: Pageable): Page<Exam>

    // Find exams by creator
    fun findByCreatorId(creatorId: Long): List<Exam>

    // Filter by status
    fun findByStatus(status: Int): List<Exam>
    fun findByStatus(status: Int, pageable: Pageable): Page<Exam>

    // Get active exams for a course
    fun findByCourseIdAndStatus(courseId: Long, status: Int): List<Exam>
    fun findByCourseIdAndStatus(courseId: Long, status: Int, pageable: Pageable): Page<Exam>

    // Check ownership
    fun existsByIdAndCreatorId(id: Long, creatorId: Long): Boolean

    // Find by course and creator
    fun findByCourseIdAndCreatorId(courseId: Long, creatorId: Long): List<Exam>

    // Find exams by course IDs (for teacher's teaching exams)
    fun findByCourseIdIn(courseIds: List<Long>): List<Exam>
    fun findByCourseIdIn(courseIds: List<Long>, pageable: Pageable): Page<Exam>
    fun findByCourseIdInAndStatus(courseIds: List<Long>, status: Int, pageable: Pageable): Page<Exam>

    // Find published exams starting within a time range (for reminder scheduling)
    fun findByStatusAndStartTimeBetween(status: Int, start: java.time.LocalDateTime, end: java.time.LocalDateTime): List<Exam>

    // Find published exams that have ended (for auto-ending)
    fun findByStatusAndEndTimeBefore(status: Int, endTime: java.time.LocalDateTime): List<Exam>

    // Unified search with optional filters
    @Query("SELECT e FROM Exam e WHERE " +
            "(:creatorId IS NULL OR e.creatorId = :creatorId) AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:courseId IS NULL OR e.courseId = :courseId)")
    fun searchExams(
        @org.springframework.data.repository.query.Param("creatorId") creatorId: Long?,
        @org.springframework.data.repository.query.Param("status") status: Int?,
        @org.springframework.data.repository.query.Param("courseId") courseId: Long?,
        pageable: Pageable
    ): Page<Exam>
}
