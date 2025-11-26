package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.QuestionBankRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionBankResponse
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.QuestionBankService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/question-banks")
@Tag(name = "题库管理", description = "题库相关接口")
class QuestionBankController(
    private val questionBankService: QuestionBankService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(
        summary = "创建题库",
        description = "教师和管理员创建新题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createQuestionBank(@Valid @RequestBody request: QuestionBankRequest): Result<QuestionBankResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建题库", 403)
        }

        return try {
            val questionBank = questionBankService.createQuestionBank(request, user.id ?: 0L)
            Result.success(questionBank, "题库创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "获取所有题库",
        description = "获取所有题库列表",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllQuestionBanks(): Result<List<QuestionBankResponse>> {
        val questionBanks = questionBankService.getAllQuestionBanks()
        return Result.success(questionBanks)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取题库详情",
        description = "根据ID获取题库详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionBankById(
        @Parameter(description = "题库ID") @PathVariable id: Long
    ): Result<QuestionBankResponse> {
        return try {
            val questionBank = questionBankService.getQuestionBankById(id)
            Result.success(questionBank)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "题库不存在", 404)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新题库",
        description = "教师更新自己的题库，管理员可更新任何题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateQuestionBank(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @Valid @RequestBody request: QuestionBankRequest
    ): Result<QuestionBankResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val questionBank = questionBankService.updateQuestionBank(id, request, user.id ?: 0L, user.role)
            Result.success(questionBank, "题库更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除题库",
        description = "教师删除自己的题库，管理员可删除任何题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteQuestionBank(
        @Parameter(description = "题库ID") @PathVariable id: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            questionBankService.deleteQuestionBank(id, user.id ?: 0L, user.role)
            Result.success("题库删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @GetMapping("/my")
    @Operation(
        summary = "获取我的题库",
        description = "获取当前用户创建的所有题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyQuestionBanks(): Result<List<QuestionBankResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val questionBanks = questionBankService.getMyQuestionBanks(user.id ?: 0L)
        return Result.success(questionBanks)
    }

    @PostMapping("/{id}/questions/{questionId}")
    @Operation(
        summary = "添加题目到题库",
        description = """
            将指定题目添加到题库中
            
            注意事项:
            - 只有题库创建者或管理员可以添加题目
            - 不能添加重复的题目
            - 题目必须存在
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun addQuestionToBank(
        @Parameter(description = "题库ID", example = "1") @PathVariable id: Long,
        @Parameter(description = "题目ID", example = "1") @PathVariable questionId: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            questionBankService.addQuestionToBank(id, questionId, user.id ?: 0L, user.role)
            Result.success("题目添加成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "添加失败", 400)
        }
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(
        summary = "从题库移除题目",
        description = "将指定题目从题库中移除",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun removeQuestionFromBank(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @Parameter(description = "题目ID") @PathVariable questionId: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            questionBankService.removeQuestionFromBank(id, questionId, user.id ?: 0L, user.role)
            Result.success("题目移除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "移除失败", 400)
        }
    }

    @GetMapping("/{id}/questions")
    @Operation(
        summary = "获取题库中的所有题目",
        description = """
            查看指定题库包含的所有题目
            
            返回结果包含:
            - 题目完整信息
            - 题目类型、难度、分类
            - 创建者信息
            - 题目被多少个题库使用
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionsInBank(
        @Parameter(description = "题库ID", example = "1") @PathVariable id: Long
    ): Result<List<QuestionResponse>> {
        return try {
            val questions = questionBankService.getQuestionsInBank(id)
            Result.success(questions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }
}
