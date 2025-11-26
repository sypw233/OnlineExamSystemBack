package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    // Find courses by teacher
    fun findByTeacherId(teacherId: Long): List<Course>
    
    // Find active courses sorted by creation time
    fun findByStatusOrderByCreateTimeDesc(status: Int): List<Course>
    
    // Check if course exists and belongs to teacher
    fun existsByIdAndTeacherId(id: Long, teacherId: Long): Boolean
    
    // Find by ID and teacher (for ownership verification)
    fun findByIdAndTeacherId(id: Long, teacherId: Long): Course?
}
