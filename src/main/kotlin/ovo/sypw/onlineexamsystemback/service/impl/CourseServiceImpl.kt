package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.CourseRequest
import ovo.sypw.onlineexamsystemback.dto.response.CourseResponse
import ovo.sypw.onlineexamsystemback.dto.response.EnrollmentResponse
import ovo.sypw.onlineexamsystemback.entity.Course
import ovo.sypw.onlineexamsystemback.entity.CourseSelection
import ovo.sypw.onlineexamsystemback.repository.CourseRepository
import ovo.sypw.onlineexamsystemback.repository.CourseSelectionRepository
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.CourseService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CourseServiceImpl(
    private val courseRepository: CourseRepository,
    private val courseSelectionRepository: CourseSelectionRepository,
    private val userRepository: UserRepository
) : CourseService {

    override fun createCourse(courseRequest: CourseRequest, teacherId: Long): CourseResponse {
        val teacher = userRepository.findById(teacherId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }
        
        // Admin and teacher can create courses
        if (teacher.role != "teacher" && teacher.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以创建课程")
        }

        val course = Course(
            courseName = courseRequest.courseName,
            description = courseRequest.description,
            teacherId = teacherId,
            status = courseRequest.status
        )

        val savedCourse = courseRepository.save(course)
        return toCourseResponse(savedCourse, teacher.realName ?: teacher.username, 0)
    }

    override fun updateCourse(id: Long, courseRequest: CourseRequest, userId: Long, userRole: String): CourseResponse {
        val course = courseRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Check permission: admin can update any course, teacher can only update their own
        if (userRole != "admin" && course.teacherId != userId) {
            throw IllegalArgumentException("您没有权限修改此课程")
        }

        course.courseName = courseRequest.courseName
        course.description = courseRequest.description
        course.status = courseRequest.status

        val updatedCourse = courseRepository.save(course)
        val teacher = userRepository.findById(course.teacherId).orElseThrow {
            throw IllegalArgumentException("教师不存在")
        }
        val enrollmentCount = courseSelectionRepository.countByCourseId(id)
        return toCourseResponse(updatedCourse, teacher.realName ?: teacher.username, enrollmentCount)
    }

    override fun deleteCourse(id: Long, userId: Long, userRole: String) {
        val course = courseRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Check permission
        if (userRole != "admin" && course.teacherId != userId) {
            throw IllegalArgumentException("您没有权限删除此课程")
        }

        // Check if course has enrollments
        val enrollmentCount = courseSelectionRepository.countByCourseId(id)
        if (enrollmentCount > 0) {
            throw IllegalArgumentException("该课程已有学生选课，无法删除")
        }

        courseRepository.delete(course)
    }

    override fun getCourseById(id: Long): CourseResponse {
        val course = courseRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        val teacher = userRepository.findById(course.teacherId).orElseThrow {
            throw IllegalArgumentException("教师不存在")
        }
        val enrollmentCount = courseSelectionRepository.countByCourseId(id)
        return toCourseResponse(course, teacher.realName ?: teacher.username, enrollmentCount)
    }

    override fun getAllActiveCourses(pageable: Pageable): Page<CourseResponse> {
        val coursePage = courseRepository.findByStatusOrderByCreateTimeDesc(1, pageable)
        val teacherIds = coursePage.content.map { it.teacherId }.toSet()
        val teachers = userRepository.findAllById(teacherIds).associateBy { it.id }
        val courseIds = coursePage.content.mapNotNull { it.id }
        val enrollmentCounts = batchFetchEnrollmentCounts(courseIds)

        return coursePage.map { course ->
            val teacher = teachers[course.teacherId] ?: throw IllegalArgumentException("教师不存在")
            toCourseResponse(course, teacher.realName ?: teacher.username, enrollmentCounts[course.id] ?: 0)
        }
    }

    override fun getMyCourses(userId: Long, userRole: String): List<CourseResponse> {
        val courses = when (userRole) {
            "teacher" -> courseRepository.findByTeacherId(userId)
            "student" -> {
                val enrollments = courseSelectionRepository.findByStudentId(userId)
                courseRepository.findAllById(enrollments.map { it.courseId })
            }
            else -> courseRepository.findAll()
        }

        val teacherIds = courses.map { it.teacherId }.toSet()
        val teachers = userRepository.findAllById(teacherIds).associateBy { it.id }
        val courseIds = courses.mapNotNull { it.id }
        val enrollmentCounts = batchFetchEnrollmentCounts(courseIds)

        return courses.map { course ->
            val teacher = teachers[course.teacherId] ?: throw IllegalArgumentException("教师不存在")
            toCourseResponse(course, teacher.realName ?: teacher.username, enrollmentCounts[course.id] ?: 0)
        }
    }

    override fun enrollStudent(courseId: Long, studentId: Long): EnrollmentResponse {
        // Check if student exists
        val student = userRepository.findById(studentId).orElseThrow {
            throw IllegalArgumentException("学生不存在")
        }

        if (student.role != "student") {
            throw IllegalArgumentException("只有学生可以选课")
        }

        // Check if course exists and is active
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        if (course.status != 1) {
            throw IllegalArgumentException("该课程当前不可选")
        }

        // Check if already enrolled
        if (courseSelectionRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw IllegalArgumentException("您已经选过该课程")
        }

        val enrollment = CourseSelection(
            studentId = studentId,
            courseId = courseId
        )

        val savedEnrollment = courseSelectionRepository.save(enrollment)
        
        return EnrollmentResponse(
            id = savedEnrollment.id ?: 0L,
            studentId = studentId,
            studentName = student.realName ?: student.username,
            courseId = courseId,
            courseName = course.courseName,
            enrollmentTime = savedEnrollment.selectionTime
        )
    }

    override fun getEnrolledStudents(courseId: Long, teacherId: Long, userRole: String): List<EnrollmentResponse> {
        // Verify teacher owns the course or is admin
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Admin can view any course, teacher can only view their own
        if (userRole != "admin" && course.teacherId != teacherId) {
            throw IllegalArgumentException("您没有权限查看该课程的选课情况")
        }

        val enrollments = courseSelectionRepository.findByCourseId(courseId)
        val studentIds = enrollments.map { it.studentId }.toSet()
        val students = userRepository.findAllById(studentIds).associateBy { it.id }

        return enrollments.map { enrollment ->
            val student = students[enrollment.studentId] ?: throw IllegalArgumentException("学生不存在")
            EnrollmentResponse(
                id = enrollment.id ?: 0L,
                studentId = enrollment.studentId,
                studentName = student.realName ?: student.username,
                courseId = courseId,
                courseName = course.courseName,
                enrollmentTime = enrollment.selectionTime
            )
        }
    }

    override fun getMyEnrollments(studentId: Long): List<EnrollmentResponse> {
        val enrollments = courseSelectionRepository.findByStudentId(studentId)
        val courseIds = enrollments.map { it.courseId }
        val courses = courseRepository.findAllById(courseIds).associateBy { it.id }
        val student = userRepository.findById(studentId).orElseThrow {
            throw IllegalArgumentException("学生不存在")
        }

        return enrollments.map { enrollment ->
            val course = courses[enrollment.courseId] ?: throw IllegalArgumentException("课程不存在")
            EnrollmentResponse(
                id = enrollment.id ?: 0L,
                studentId = studentId,
                studentName = student.realName ?: student.username,
                courseId = enrollment.courseId,
                courseName = course.courseName,
                enrollmentTime = enrollment.selectionTime
            )
        }
    }

    override fun addStudentToCourse(
        courseId: Long,
        studentId: Long,
        operatorId: Long,
        operatorRole: String
    ): EnrollmentResponse {
        // Check course exists
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Check permission: admin or course teacher
        if (operatorRole != "admin" && course.teacherId != operatorId) {
            throw IllegalArgumentException("您没有权限为此课程添加学生")
        }

        // Check student exists and is actually a student
        val student = userRepository.findById(studentId).orElseThrow {
            throw IllegalArgumentException("学生不存在")
        }
        if (student.role != "student") {
            throw IllegalArgumentException("只能添加学生角色到课程")
        }

        // Check course is active
        if (course.status != 1) {
            throw IllegalArgumentException("该课程当前不可选课")
        }

        // Check if already enrolled
        if (courseSelectionRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw IllegalArgumentException("该学生已选此课程")
        }

        val enrollment = CourseSelection(
            studentId = studentId,
            courseId = courseId
        )
        val savedEnrollment = courseSelectionRepository.save(enrollment)

        return EnrollmentResponse(
            id = savedEnrollment.id ?: 0L,
            studentId = studentId,
            studentName = student.realName ?: student.username,
            courseId = courseId,
            courseName = course.courseName,
            enrollmentTime = savedEnrollment.selectionTime
        )
    }

    override fun removeStudentFromCourse(
        courseId: Long,
        studentId: Long,
        operatorId: Long,
        operatorRole: String
    ) {
        // Check course exists
        val course = courseRepository.findById(courseId).orElseThrow {
            throw IllegalArgumentException("课程不存在")
        }

        // Check permission:
        // - admin can remove anyone
        // - teacher can remove from their own course
        // - student can drop their own course
        val hasPermission = when (operatorRole) {
            "admin" -> true
            "teacher" -> course.teacherId == operatorId
            "student" -> operatorId == studentId
            else -> false
        }
        if (!hasPermission) {
            throw IllegalArgumentException("您没有权限移除此学生")
        }

        // Find enrollment
        val enrollment = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId)
            ?: throw IllegalArgumentException("该学生未选此课程")

        courseSelectionRepository.delete(enrollment)
    }

    private fun toCourseResponse(course: Course, teacherName: String, enrollmentCount: Long): CourseResponse {
        return CourseResponse(
            id = course.id ?: 0L,
            courseName = course.courseName,
            description = course.description,
            teacherId = course.teacherId,
            teacherName = teacherName,
            status = course.status,
            enrollmentCount = enrollmentCount,
            createTime = course.createTime
        )
    }

    private fun batchFetchEnrollmentCounts(courseIds: List<Long>): Map<Long, Long> {
        return courseSelectionRepository.countByCourseIdIn(courseIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }
    }
}
