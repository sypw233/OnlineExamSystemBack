package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.media.Schema
import ovo.sypw.onlineexamsystemback.dto.request.QuestionRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.QuestionService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/questions")
@Tag(name = "题目管理", description = "题目相关接口")
class QuestionController(
    private val questionService: QuestionService
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
        @Valid @RequestBody request: QuestionRequest,
        @CurrentUser user: User
    ): Result<QuestionResponse> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建题目", 403)
        }

        return try {
            val question = questionService.createQuestion(request, user.safeId)
            Result.success(question, "题目创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "查询题目列表",
        description = "管理员获取全部题目，教师仅获取自己创建的题目。支持按类型、难度、分类筛选，支持分页。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllQuestions(
        @Parameter(description = "页码", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "题目类型", schema = Schema(allowableValues = ["single", "multiple", "true_false", "fill_blank", "short_answer"]))
        @RequestParam(required = false) type: String?,
        @Parameter(description = "难度等级", schema = Schema(allowableValues = ["easy", "medium", "hard"]))
        @RequestParam(required = false) difficulty: String?,
        @Parameter(description = "分类")
        @RequestParam(required = false) category: String?,
        @CurrentUser user: User
    ): Result<Page<QuestionResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val creatorId = if (user.role == "admin") null else user.id
        val questions = questionService.searchQuestions(
            creatorId = creatorId,
            type = type,
            difficulty = difficulty,
            category = category,
            pageable = pageable
        )
        return Result.success(questions)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取题目详情",
        description = "根据ID获取题目详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionById(
        @Parameter(description = "题目ID") @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<QuestionResponse> {
        return try {
            val question = questionService.getQuestionById(id)
            // Students must not see answer or analysis
            val sanitized = if (user.role == "student") {
                question.copy(answer = null, analysis = null)
            } else question
            Result.success(sanitized)
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
        @Valid @RequestBody request: QuestionRequest,
        @CurrentUser user: User
    ): Result<QuestionResponse> {
        return try {
            val question = questionService.updateQuestion(id, request, user.safeId, user.role)
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
        @Parameter(description = "题目ID") @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<String> {
        return try {
            questionService.deleteQuestion(id, user.safeId, user.role)
            Result.success("题目删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

}
