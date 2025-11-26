package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.QuestionRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.QuestionService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/questions")
@Tag(name = "题目管理", description = "题目相关接口")
class QuestionController(
    private val questionService: QuestionService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(
        summary = "创建题目",
        description = """
            教师和管理员创建新题目
            
            支持的题目类型:
            - single: 单选题 (必须提供options)
            - multiple: 多选题 (必须提供options)
            - true_false: 判断题
            - fill_blank: 填空题
            - short_answer: 简答题
            
            难度等级: easy(简单), medium(中等), hard(困难)
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createQuestion(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "题目信息",
            required = true
        )
        @Valid @RequestBody request: QuestionRequest
    ): Result<QuestionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建题目", 403)
        }

        return try {
            val question = questionService.createQuestion(request, user.id ?: 0L)
            Result.success(question, "题目创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "获取所有题目",
        description = "获取所有题目列表",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllQuestions(): Result<List<QuestionResponse>> {
        val questions = questionService.getAllQuestions()
        return Result.success(questions)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取题目详情",
        description = "根据ID获取题目详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionById(
        @Parameter(description = "题目ID") @PathVariable id: Long
    ): Result<QuestionResponse> {
        return try {
            val question = questionService.getQuestionById(id)
            Result.success(question)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "题目不存在", 404)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新题目",
        description = "教师更新自己的题目，管理员可更新任何题目",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateQuestion(
        @Parameter(description = "题目ID") @PathVariable id: Long,
        @Valid @RequestBody request: QuestionRequest
    ): Result<QuestionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val question = questionService.updateQuestion(id, request, user.id ?: 0L, user.role)
            Result.success(question, "题目更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除题目",
        description = "教师删除自己的题目，管理员可删除任何题目",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteQuestion(
        @Parameter(description = "题目ID") @PathVariable id: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            questionService.deleteQuestion(id, user.id ?: 0L, user.role)
            Result.success("题目删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @GetMapping("/my")
    @Operation(
        summary = "获取我的题目",
        description = "获取当前用户创建的所有题目",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyQuestions(): Result<List<QuestionResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val questions = questionService.getMyQuestions(user.id ?: 0L)
        return Result.success(questions)
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "按类型筛选题目",
        description = "获取指定类型的题目列表。可选值: single, multiple, true_false, fill_blank, short_answer",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionsByType(
        @Parameter(
            description = "题目类型",
            example = "single",
            schema = io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = ["single", "multiple", "true_false", "fill_blank", "short_answer"]
            )
        )
        @PathVariable type: String
    ): Result<List<QuestionResponse>> {
        val questions = questionService.getQuestionsByType(type)
        return Result.success(questions)
    }

    @GetMapping("/difficulty/{difficulty}")
    @Operation(
        summary = "按难度筛选题目",
        description = "获取指定难度的题目列表。可选值: easy(简单), medium(中等), hard(困难)",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionsByDifficulty(
        @Parameter(
            description = "难度等级",
            example = "easy",
            schema = io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = ["easy", "medium", "hard"]
            )
        )
        @PathVariable difficulty: String
    ): Result<List<QuestionResponse>> {
        val questions = questionService.getQuestionsByDifficulty(difficulty)
        return Result.success(questions)
    }

    @GetMapping("/category/{category}")
    @Operation(
        summary = "按分类筛选题目",
        description = "获取指定分类的题目列表",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionsByCategory(
        @Parameter(description = "分类") @PathVariable category: String
    ): Result<List<QuestionResponse>> {
        val questions = questionService.getQuestionsByCategory(category)
        return Result.success(questions)
    }
}
