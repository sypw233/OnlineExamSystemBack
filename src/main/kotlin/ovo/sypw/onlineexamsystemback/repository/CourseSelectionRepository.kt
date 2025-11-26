package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.CourseSelection
import org.springframework.data.jpa.repository.JpaRepository
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
    
    // Get enrollment by student and course
    fun findByStudentIdAndCourseId(studentId: Long, courseId: Long): CourseSelection?
}
