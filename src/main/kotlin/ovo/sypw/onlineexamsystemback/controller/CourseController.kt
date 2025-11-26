package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.CourseRequest
import ovo.sypw.onlineexamsystemback.dto.response.CourseResponse
import ovo.sypw.onlineexamsystemback.dto.response.EnrollmentResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.CourseService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses")
@Tag(name = "课程管理", description = "课程相关接口")
class CourseController(
    private val courseService: CourseService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(
        summary = "创建课程",
        description = "教师和管理员创建新课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createCourse(@Valid @RequestBody courseRequest: CourseRequest): Result<CourseResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建课程", 403)
        }

        val course = courseService.createCourse(courseRequest, user.id ?: 0L)
        return Result.success(course, "课程创建成功")
    }

    @GetMapping
    @Operation(summary = "获取所有活跃课程", description = "获取所有状态为活跃的课程列表")
    fun getAllActiveCourses(): Result<List<CourseResponse>> {
        val courses = courseService.getAllActiveCourses()
        return Result.success(courses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取课程详情", description = "根据ID获取课程详细信息")
    fun getCourseById(
        @Parameter(description = "课程ID") @PathVariable id: Long
    ): Result<CourseResponse> {
        return try {
            val course = courseService.getCourseById(id)
            Result.success(course)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "课程不存在", 404)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新课程",
        description = "教师更新自己的课程，管理员可更新任何课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateCourse(
        @Parameter(description = "课程ID") @PathVariable id: Long,
        @Valid @RequestBody courseRequest: CourseRequest
    ): Result<CourseResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val course = courseService.updateCourse(id, courseRequest, user.id ?: 0L, user.role)
            Result.success(course, "课程更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除课程",
        description = "教师删除自己的课程，管理员可删除任何课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteCourse(
        @Parameter(description = "课程ID") @PathVariable id: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            courseService.deleteCourse(id, user.id ?: 0L, user.role)
            Result.success("课程删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @PostMapping("/{id}/enroll")
    @Operation(
        summary = "选课",
        description = "学生选择课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun enrollCourse(
        @Parameter(description = "课程ID") @PathVariable id: Long
    ): Result<EnrollmentResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "student") {
            return Result.error("只有学生可以选课", 403)
        }

        return try {
            val enrollment = courseService.enrollStudent(id, user.id ?: 0L)
            Result.success(enrollment, "选课成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "选课失败", 400)
        }
    }

    @GetMapping("/my")
    @Operation(
        summary = "获取我的课程",
        description = "教师获取自己创建的课程，学生获取已选课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyCourses(): Result<List<CourseResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val courses = courseService.getMyCourses(user.id ?: 0L, user.role)
        return Result.success(courses)
    }

    @GetMapping("/{id}/students")
    @Operation(
        summary = "获取选课学生列表",
        description = "教师查看自己课程的选课学生，管理员可查看所有课程",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getEnrolledStudents(
        @Parameter(description = "课程ID") @PathVariable id: Long
    ): Result<List<EnrollmentResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以查看选课情况", 403)
        }

        return try {
            val enrollments = courseService.getEnrolledStudents(id, user.id ?: 0L, user.role)
            Result.success(enrollments)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }

    @GetMapping("/my-enrollments")
    @Operation(
        summary = "获取我的选课记录",
        description = "学生查看自己的所有选课记录",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyEnrollments(): Result<List<EnrollmentResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "student") {
            return Result.error("只有学生可以查看选课记录", 403)
        }

        val enrollments = courseService.getMyEnrollments(user.id ?: 0L)
        return Result.success(enrollments)
    }
}
