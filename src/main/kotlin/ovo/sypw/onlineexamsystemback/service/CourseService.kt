package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.CourseRequest
import ovo.sypw.onlineexamsystemback.dto.response.CourseResponse
import ovo.sypw.onlineexamsystemback.dto.response.EnrollmentResponse

interface CourseService {
    // Course CRUD
    fun createCourse(courseRequest: CourseRequest, teacherId: Long): CourseResponse
    fun updateCourse(id: Long, courseRequest: CourseRequest, userId: Long, userRole: String): CourseResponse
    fun deleteCourse(id: Long, userId: Long, userRole: String)
    fun getCourseById(id: Long): CourseResponse
    fun getAllActiveCourses(): List<CourseResponse>
    fun getMyCourses(userId: Long, userRole: String): List<CourseResponse>
    
    // Enrollment
    fun enrollStudent(courseId: Long, studentId: Long): EnrollmentResponse
    fun getEnrolledStudents(courseId: Long, teacherId: Long, userRole: String): List<EnrollmentResponse>
    fun getMyEnrollments(studentId: Long): List<EnrollmentResponse>
}
