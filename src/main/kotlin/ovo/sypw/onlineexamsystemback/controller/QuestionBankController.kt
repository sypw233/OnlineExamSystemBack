package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.QuestionBankRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionBankResponse
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.QuestionBankService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/question-banks")
@Tag(name = "题库管理", description = "题库相关接口")
class QuestionBankController(
    private val questionBankService: QuestionBankService
) {

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping
    @Operation(
        summary = "创建题库",
        description = "教师和管理员创建新题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createQuestionBank(
        @Valid @RequestBody request: QuestionBankRequest,
        @CurrentUser user: User
    ): Result<QuestionBankResponse> {
        return try {
            val questionBank = questionBankService.createQuestionBank(request, user.safeId)
            Result.success(questionBank, "题库创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "查询题库列表",
        description = "管理员可查询所有题库并按关键字、创建者筛选；教师仅查看自己创建的题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllQuestionBanks(
        @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "关键字搜索(匹配题库名称、描述)") @RequestParam(required = false) keyword: String?,
        @CurrentUser user: User
    ): Result<Page<QuestionBankResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val questionBanks = if (user.role == "admin") {
            // Admin can search all banks with keyword filter
            questionBankService.searchQuestionBanks(keyword, null, pageable)
        } else {
            // Teacher: only their own banks
            questionBankService.getMyQuestionBanks(user.safeId, pageable)
        }
        return Result.success(questionBanks)
    }


    @GetMapping("/{id:\\d+}")
    @Operation(
        summary = "获取题库详情",
        description = "根据ID获取题库详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getQuestionBankById(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<QuestionBankResponse> {
        return try {
            val questionBank = questionBankService.getQuestionBankById(id)
            Result.success(questionBank)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "题库不存在", 404)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(
        description = "教师更新自己的题库，管理员可更新任何题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateQuestionBank(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @Valid @RequestBody request: QuestionBankRequest,
        @CurrentUser user: User
    ): Result<QuestionBankResponse> {
        return try {
            val questionBank = questionBankService.updateQuestionBank(id, request, user.safeId, user.role)
            Result.success(questionBank, "题库更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
        description = "教师删除自己的题库，管理员可删除任何题库",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteQuestionBank(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<String> {
        return try {
            questionBankService.deleteQuestionBank(id, user.safeId, user.role)
            Result.success("题库删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }



    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
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
        @Parameter(description = "题目ID", example = "1") @PathVariable questionId: Long,
        @CurrentUser user: User
    ): Result<String> {
        return try {
            questionBankService.addQuestionToBank(id, questionId, user.safeId, user.role)
            Result.success("题目添加成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "添加失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(
        summary = "从题库移除题目",
        description = "将指定题目从题库中移除",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun removeQuestionFromBank(
        @Parameter(description = "题库ID") @PathVariable id: Long,
        @Parameter(description = "题目ID") @PathVariable questionId: Long,
        @CurrentUser user: User
    ): Result<String> {
        return try {
            questionBankService.removeQuestionFromBank(id, questionId, user.safeId, user.role)
            Result.success("题目移除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "移除失败", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
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
        @Parameter(description = "题库ID", example = "1") @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<List<QuestionResponse>> {
        return try {
            val questions = questionBankService.getQuestionsInBank(id)
            Result.success(questions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }
}
