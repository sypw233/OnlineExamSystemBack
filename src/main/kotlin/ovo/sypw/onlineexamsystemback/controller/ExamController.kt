package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.ExamService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "考试相关接口")
class ExamController(
    private val examService: ExamService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(
        summary = "创建考试",
        description = """
            教师和管理员创建新考试
            
            监考控制功能:
            - allowedPlatforms: 限制考试平台 (desktop/mobile/both)
            - strictMode: 开启严格监考
            - maxSwitchCount: 最大切出次数
            - fullscreenRequired: 强制全屏（仅桌面端）
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createExam(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "考试信息",
            required = true
        )
        @Valid @RequestBody request: ExamRequest
    ): Result<ExamResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建考试", 403)
        }

        return try {
            val exam = examService.createExam(request, user.id ?: 0L)
            Result.success(exam, "考试创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "获取所有考试",
        description = "获取所有考试列表",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllExams(): Result<List<ExamResponse>> {
        val exams = examService.getAllExams()
        return Result.success(exams)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取考试详情",
        description = "根据ID获取考试详细信息（包含监考设置）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamById(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<ExamResponse> {
        return try {
            val exam = examService.getExamById(id)
            Result.success(exam)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "考试不存在", 404)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新考试",
        description = "教师更新自己的考试，管理员可更新任何考试。只有草稿状态的考试可以修改。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateExam(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: ExamRequest
    ): Result<ExamResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val exam = examService.updateExam(id, request, user.id ?: 0L, user.role)
            Result.success(exam, "考试更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除考试",
        description = "教师删除自己的考试，管理员可删除任何考试",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteExam(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            examService.deleteExam(id, user.id ?: 0L, user.role)
            Result.success("考试删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @GetMapping("/my")
    @Operation(
        summary = "获取我的考试",
        description = "获取当前用户创建的所有考试",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyExams(): Result<List<ExamResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val exams = examService.getMyExams(user.id ?: 0L)
        return Result.success(exams)
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "按状态筛选考试",
        description = "获取指定状态的考试列表。0-草稿, 1-已发布, 2-已结束",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamsByStatus(
        @Parameter(
            description = "考试状态",
            example = "1",
            schema = io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = ["0", "1", "2"]
            )
        )
        @PathVariable status: Int
    ): Result<List<ExamResponse>> {
        val exams = examService.getExamsByStatus(status)
        return Result.success(exams)
    }

    @GetMapping("/course/{courseId}")
    @Operation(
        summary = "获取课程的所有考试",
        description = "查看指定课程的所有考试",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamsByCourse(
        @Parameter(description = "课程ID", example = "1") @PathVariable courseId: Long
    ): Result<List<ExamResponse>> {
        return try {
            val exams = examService.getExamsByCourse(courseId)
            Result.success(exams)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }

    @PostMapping("/{id}/publish")
    @Operation(
        summary = "发布考试",
        description = """
            将草稿状态的考试发布为可参加状态
            
            发布前验证:
            - 至少包含一道题目
            - 考试时间未过期
            - 监考设置正确
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun publishExam(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<ExamResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val exam = examService.publishExam(id, user.id ?: 0L, user.role)
            Result.success(exam, "考试发布成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "发布失败", 400)
        }
    }

    @PostMapping("/{id}/questions")
    @Operation(
        summary = "添加题目到考试",
        description = """
            将指定题目添加到考试中（仅草稿状态）
            
            注意:
            - 需要指定题目分值和显示顺序
            - 不能添加重复题目
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun addQuestionToExam(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: ExamQuestionRequest
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            examService.addQuestionToExam(id, request, user.id ?: 0L, user.role)
            Result.success("题目添加成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "添加失败", 400)
        }
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(
        summary = "从考试移除题目",
        description = "将指定题目从考试中移除（仅草稿状态）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun removeQuestionFromExam(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Parameter(description = "题目ID", example = "1") @PathVariable questionId: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            examService.removeQuestionFromExam(id, questionId, user.id ?: 0L, user.role)
            Result.success("题目移除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "移除失败", 400)
        }
    }

    @GetMapping("/{id}/questions")
    @Operation(
        summary = "获取考试的所有题目",
        description = "查看指定考试包含的所有题目（按顺序排列）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamQuestions(
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<List<ExamQuestionResponse>> {
        return try {
            val questions = examService.getExamQuestions(id)
            Result.success(questions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }
}
