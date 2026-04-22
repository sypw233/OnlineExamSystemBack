package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.CourseRequest
import ovo.sypw.onlineexamsystemback.dto.response.CourseResponse
import ovo.sypw.onlineexamsystemback.dto.response.EnrollmentResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CourseService {
    // Course CRUD
    fun createCourse(courseRequest: CourseRequest, teacherId: Long): CourseResponse
    fun updateCourse(id: Long, courseRequest: CourseRequest, userId: Long, userRole: String): CourseResponse
    fun deleteCourse(id: Long, userId: Long, userRole: String)
    fun getCourseById(id: Long): CourseResponse
    fun getAllActiveCourses(pageable: Pageable): Page<CourseResponse>
    fun getMyCourses(userId: Long, userRole: String): List<CourseResponse>
    
    // Enrollment
    fun enrollStudent(courseId: Long, studentId: Long): EnrollmentResponse
    fun getEnrolledStudents(courseId: Long, teacherId: Long, userRole: String): List<EnrollmentResponse>
    fun getMyEnrollments(studentId: Long): List<EnrollmentResponse>

    // Admin/Teacher manage students
    fun addStudentToCourse(courseId: Long, studentId: Long, operatorId: Long, operatorRole: String): EnrollmentResponse
    fun removeStudentFromCourse(courseId: Long, studentId: Long, operatorId: Long, operatorRole: String)
}
