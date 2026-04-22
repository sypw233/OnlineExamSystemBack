package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.CourseSelection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CourseSelectionRepository : JpaRepository<CourseSelection, Long> {
    // Find all enrollments by student
    fun findByStudentId(studentId: Long): List<CourseSelection>
    
    // Find all students enrolled in a course
    fun findByCourseId(courseId: Long): List<CourseSelection>
    
    // Check if student is already enrolled in course
    fun existsByStudentIdAndCourseId(studentId: Long, courseId: Long): Boolean
    
    // Count students enrolled in a course
    fun countByCourseId(courseId: Long): Long

    // Batch count enrollments for multiple courses
    @Query("SELECT cs.courseId, COUNT(cs) FROM CourseSelection cs WHERE cs.courseId IN :courseIds GROUP BY cs.courseId")
    fun countByCourseIdIn(@org.springframework.data.repository.query.Param("courseIds") courseIds: List<Long>): List<Array<Any>>

    // Get enrollment by student and course
    fun findByStudentIdAndCourseId(studentId: Long, courseId: Long): CourseSelection?
}
