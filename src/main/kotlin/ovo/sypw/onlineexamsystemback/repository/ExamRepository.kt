package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Exam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExamRepository : JpaRepository<Exam, Long> {
    // Find exams by course
    fun findByCourseId(courseId: Long): List<Exam>
    
    // Find exams by creator
    fun findByCreatorId(creatorId: Long): List<Exam>
    
    // Filter by status
    fun findByStatus(status: Int): List<Exam>
    
    // Get active exams for a course
    fun findByCourseIdAndStatus(courseId: Long, status: Int): List<Exam>
    
    // Check ownership
    fun existsByIdAndCreatorId(id: Long, creatorId: Long): Boolean
    
    // Find by course and creator
    fun findByCourseIdAndCreatorId(courseId: Long, creatorId: Long): List<Exam>
}
