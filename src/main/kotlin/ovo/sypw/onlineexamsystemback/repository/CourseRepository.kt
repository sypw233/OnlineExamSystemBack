package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Course
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    // Find courses by teacher
    fun findByTeacherId(teacherId: Long): List<Course>

    // Find active courses sorted by creation time
    fun findByStatusOrderByCreateTimeDesc(status: Int): List<Course>
    fun findByStatusOrderByCreateTimeDesc(status: Int, pageable: Pageable): Page<Course>

    // Check if course exists and belongs to teacher
    fun existsByIdAndTeacherId(id: Long, teacherId: Long): Boolean

    // Find by ID and teacher (for ownership verification)
    fun findByIdAndTeacherId(id: Long, teacherId: Long): Course?

    // Search courses with filters
    @Query("SELECT c FROM Course c WHERE " +
            "(:keyword IS NULL OR c.courseName LIKE %:keyword% OR c.description LIKE %:keyword%) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:teacherId IS NULL OR c.teacherId = :teacherId)")
    fun searchCourses(
        @Param("keyword") keyword: String?,
        @Param("status") status: Int?,
        @Param("teacherId") teacherId: Long?,
        pageable: Pageable
    ): Page<Course>
}
