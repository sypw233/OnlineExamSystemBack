package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.ExamScoreExportRequest
import ovo.sypw.onlineexamsystemback.dto.response.*
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.CourseRepository
import ovo.sypw.onlineexamsystemback.repository.ExamRepository
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.StatisticsService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "统计分析", description = "数据统计和分析接口")
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val userRepository: UserRepository,
    private val examRepository: ExamRepository,
    private val courseRepository: CourseRepository
) {

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/exam/{examId}")
    @Operation(
        summary = "考试统计",
        description = """
            获取考试的详细统计数据
            
            ## 统计内容
            - 参与人数和完成率
            - 平均分、最高分、最低分
            - 及格率（60分及以上）
            - 分数段分布（0-59, 60-69, 70-79, 80-89, 90-100）
            
            ## 权限
            - 教师可以查看自己的考试统计
            - 管理员可以查看所有考试统计
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamStatistics(
        @Parameter(description = "考试ID", example = "1")
        @PathVariable examId: Long,
        @CurrentUser user: User
    ): Result<ExamStatisticsResponse> {
        // Ownership check: teacher can only view their own exam statistics
        if (user.role == "teacher") {
            val exam = examRepository.findById(examId).orElse(null)
                ?: return Result.error("考试不存在", 404)
            
            if (exam.creatorId != user.id) {
                return Result.error("您没有权限查看此考试的统计数据", 403)
            }
        }

        return try {
            val statistics = statisticsService.getExamStatistics(examId)
            Result.success(statistics)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "获取统计数据失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/course/{courseId}")
    @Operation(
        summary = "课程统计",
        description = """
            获取课程的统计数据
            
            ## 统计内容
            - 学生总数
            - 考试总数
            - 平均成绩
            - 最高分和最低分
            
            ## 权限
            - 教师可以查看自己的课程统计
            - 管理员可以查看所有课程统计
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getCourseStatistics(
        @Parameter(description = "课程ID", example = "1")
        @PathVariable courseId: Long,
        @CurrentUser user: User
    ): Result<CourseStatisticsResponse> {
        // Ownership check: teacher can only view their own course statistics
        if (user.role == "teacher") {
            val course = courseRepository.findById(courseId).orElse(null)
                ?: return Result.error("课程不存在", 404)
            if (course.teacherId != user.id) {
                return Result.error("您没有权限查看此课程的统计数据", 403)
            }
        }

        return try {
            val statistics = statisticsService.getCourseStatistics(courseId)
            Result.success(statistics)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "获取统计数据失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/question/{questionId}")
    @Operation(
        summary = "题目统计",
        description = """
            获取题目的使用和正确率统计
            
            ## 统计内容
            - 题目使用次数
            - 总答题次数
            - 正确次数和正确率
            - 选项分布（客观题）
            
            ## 权限
            - 教师和管理员可以查看
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionStatistics(
        @Parameter(description = "题目ID", example = "1")
        @PathVariable questionId: Long,
        @CurrentUser user: User
    ): Result<QuestionStatisticsResponse> {
        return try {
            val statistics = statisticsService.getQuestionStatistics(questionId)
            Result.success(statistics)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "获取统计数据失败", 400)
        }
    }

    @GetMapping("/student/{studentId}")
    @Operation(
        summary = "学生成绩统计",
        description = """
            获取学生的成绩历史和统计分析
            
            ## 统计内容
            - 参加考试总数
            - 平均成绩
            - 最高分和最低分
            - 历史成绩列表
            
            ## 权限
            - 学生只能查看自己的统计
            - 教师和管理员可以查看所有学生统计
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getStudentStatistics(
        @Parameter(description = "学生ID", example = "1")
        @PathVariable studentId: Long,
        @CurrentUser user: User
    ): Result<StudentStatisticsResponse> {
        // Students can only view their own statistics
        if (user.role == "student" && user.id != studentId) {
            return Result.error("您只能查看自己的成绩统计", 403)
        }

        return try {
            val statistics = statisticsService.getStudentStatistics(studentId)
            Result.success(statistics)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "获取统计数据失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    @Operation(
        summary = "系统总览",
        description = """
            获取系统整体统计数据（管理员专用）
            
            ## 统计内容
            - 用户总数（按角色分类）
            - 课程总数
            - 考试总数
            - 题库总题数
            - 提交总数
            
            ## 权限
            - 仅管理员可以访问
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getSystemOverview(
        @CurrentUser user: User
    ): Result<SystemOverviewResponse> {
        return try {
            val overview = statisticsService.getSystemOverview()
            Result.success(overview)
        } catch (e: Exception) {
            Result.error(e.message ?: "获取系统总览失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping("/exam/{examId}/export")
    @Operation(
        summary = "导出考试成绩",
        description = """
            导出某场考试的学生成绩为 Excel 文件
            
            ## 权限
            - 教师可以导出自己的考试
            - 管理员可以导出任何考试
            
            ## 自定义字段
            - 学号、姓名、得分、提交时间、状态
            - 切出次数、监考异常标记
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun exportExamScores(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable examId: Long,
        @Valid @RequestBody config: ExamScoreExportRequest
    ): ResponseEntity<ByteArray> {
        // Permission check
        val exam = examRepository.findById(examId).orElse(null)
            ?: return ResponseEntity.status(404).body("考试不存在".toByteArray())

        if (user.role != "admin" && exam.creatorId != user.id) {
            return ResponseEntity.status(403).body("您没有权限导出此考试的成绩".toByteArray())
        }

        return try {
            val excelBytes = statisticsService.exportExamScores(examId, config)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                setContentDispositionFormData("attachment", "scores_exam_${examId}.xlsx")
            }
            ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(400).body((e.message ?: "导出失败").toByteArray())
        } catch (e: Exception) {
            ResponseEntity.status(500).body("导出失败: ${e.message}".toByteArray())
        }
    }
}
