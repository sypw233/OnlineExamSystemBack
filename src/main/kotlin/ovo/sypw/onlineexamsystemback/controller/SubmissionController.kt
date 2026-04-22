package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.media.Schema
import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.SubmissionService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/submissions")
@Tag(name = "考试提交管理", description = "考试监考提交和评分相关接口")
class SubmissionController(
    private val submissionService: SubmissionService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(
        summary = "提交考试答案",
        description = """
            学生提交考试答案，系统自动评分客观题
            
            ## 自动评分规则
            - **单选题/判断题**: 完全匹配才给分
            - **多选题**: 
              * 全对: 满分
              * 少选（答案是正确答案的子集）: 一半分
              * 多选/错选: 0分
            - **填空题/简答题**: 自动给0分，需要教师手动评分
            
            ## 答案格式
            - 单选: `"A"` 或 `"B"`
            - 多选: `"A,B,C"` (逗号分隔，无空格)
            - 判断: `"true"` 或 `"false"`
            - 填空/简答: 任意文本字符串
            
            ## 请求示例
            ```json
            {
              "examId": 1,
              "answers": {
                "1": "A",
                "2": "B,C",
                "3": "true",
                "4": "填空答案",
                "5": "这是简答题的详细回答"
              }
            }
            ```
            
            注意: 监考事件（切屏、退出全屏等）通过 `/api/submissions/proctoring` 接口单独记录
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun submitExam(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "考试答案数据",
            required = true
        )
        @Valid @RequestBody request: SubmissionRequest
    ): Result<SubmissionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val submission = submissionService.submitExam(request, user.id ?: 0L)
            Result.success(submission, "提交成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "提交失败", 400)
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取提交详情",
        description = "学生查看自己的提交，教师/管理员可查看任何提交",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getSubmissionById(
        @Parameter(description = "提交ID", example = "1") @PathVariable id: Long
    ): Result<SubmissionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val submission = submissionService.getSubmissionById(id, user.id ?: 0L, user.role)
            Result.success(submission)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "查询提交记录",
        description = "按考试或学生查询提交记录。学生只能查看自己的成绩，教师/管理员可查看任何记录。支持分页。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getSubmissions(
        @Parameter(description = "考试ID") @RequestParam(required = false) examId: Long?,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: Long?,
        @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") size: Int
    ): Result<Page<SubmissionResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val pageable = PageRequest.of(page, size.coerceAtMost(100))

        // Student can only view their own
        if (user.role == "student") {
            if (examId != null) {
                // Check if student has a submission for this exam
                return try {
                    val submission = submissionService.getUserSubmissions(user.id ?: 0L, pageable)
                        .content.firstOrNull { it.examId == examId }
                        ?.let { org.springframework.data.domain.PageImpl(listOf(it), pageable, 1) }
                        ?: org.springframework.data.domain.PageImpl(emptyList<SubmissionResponse>(), pageable, 0)
                    Result.success(submission)
                } catch (e: IllegalArgumentException) {
                    Result.error(e.message ?: "查询失败", 400)
                }
            }
            return try {
                val submissions = submissionService.getUserSubmissions(user.id ?: 0L, pageable)
                Result.success(submissions)
            } catch (e: IllegalArgumentException) {
                Result.error(e.message ?: "查询失败", 400)
            }
        }

        // Teacher / Admin
        if (examId != null) {
            return try {
                val submissions = submissionService.getExamSubmissions(examId, user.id ?: 0L, user.role, pageable)
                Result.success(submissions)
            } catch (e: IllegalArgumentException) {
                Result.error(e.message ?: "查询失败", 400)
            }
        }

        if (userId != null) {
            return try {
                val submissions = submissionService.getUserSubmissions(userId, pageable)
                Result.success(submissions)
            } catch (e: IllegalArgumentException) {
                Result.error(e.message ?: "查询失败", 400)
            }
        }

        return Result.error("请提供 examId 或 userId 参数", 400)
    }

    @PostMapping("/{id}/grade")
    @Operation(
        summary = "主观题手动评分",
        description = """
            教师为主观题（填空题、简答题）进行人工评分
            
            评分后状态变为"已评分"，总分重新计算
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun gradeSubmission(
        @Parameter(description = "提交ID", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: GradeRequest
    ): Result<SubmissionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以评分", 403)
        }

        return try {
            val submission = submissionService.gradeSubmission(id, request, user.id ?: 0L, user.role)
            Result.success(submission, "评分成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "评分失败", 400)
        }
    }

    @PostMapping("/proctoring")
    @Operation(
        summary = "记录监考事件",
        description = """
            前端实时记录学生违规行为（切出、退出全屏等）
            
            ## 功能说明
            - 系统会自动根据考试ID和当前用户找到对应的提交记录
            - 如果用户尚未开始考试，系统会自动创建答题记录（状态=答题中）
            - 每次记录事件时，切出次数自动+1
            
            ## 自动处理
            - 记录详细的违规事件（时间戳、类型、详情）
            - 累计切出次数
            - 如果超过考试设置的最大切出次数，自动提交考试
            
            ## 请求示例
            ```json
            {
              "examId": 1,
              "eventType": "tab_switch",
              "detail": "用户切换到了其他浏览器标签"
            }
            ```
            
            ## 响应说明
            - `recorded`: true 表示事件已记录
            - `autoSubmitted`: true 表示因超出限制已自动提交考试
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun recordProctoringEvent(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "监考事件数据",
            required = true
        )
        @Valid @RequestBody request: ProctoringEventRequest
    ): Result<Map<String, Any>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val autoSubmitted = submissionService.recordProctoringEvent(request, user.id ?: 0L)
            Result.success(
                mapOf(
                    "recorded" to true,
                    "autoSubmitted" to autoSubmitted
                ),
                if (autoSubmitted) "已超出最大切出次数，考试已自动提交" else "事件已记录"
            )
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "记录失败", 400)
        }
    }
}
