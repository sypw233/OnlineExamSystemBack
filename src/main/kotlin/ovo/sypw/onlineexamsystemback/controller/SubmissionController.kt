package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.SubmissionService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/submissions")
@Tag(name = "考试提交管理", description = "考试监考提交和评分相关接口")
class SubmissionController(
    private val submissionService: SubmissionService,
    private val userRepository: UserRepository
) {

    @PostMapping("/start")
    @Operation(
        summary = "开始考试",
        description = """
            学生点击"开始考试"按钮时调用此接口
            
            ## 功能说明
            - 创建考试答题记录（状态=答题中）
            - 记录开始时间
            - 验证考试有效性（已发布、时间范围内）
            - 如果已经开始过，返回现有记录
            
            ## 验证规则
            - 考试必须已发布（status=1）
            - 当前时间必须在考试时间范围内
            - 每个学生每场考试只能有一条答题记录
            
            ## 请求示例
            ```
            POST /api/submissions/start?examId=1
            ```
            
            ## 响应
            返回创建的答题记录，包含：
            - submissionId: 答题记录ID
            - status: 0（答题中）
            - startTime: 开始时间
            - examTitle: 考试标题
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun startExam(
        @Parameter(
            description = "考试ID",
            example = "1",
            required = true
        )
        @RequestParam examId: Long
    ): Result<SubmissionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val submission = submissionService.startExam(examId, user.id ?: 0L)
            Result.success(submission, "考试已开始")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "开始失败", 400)
        }
    }

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

    @GetMapping("/exam/{examId}")
    @Operation(
        summary = "获取考试的所有提交记录",
        description = "教师查看自己考试的提交，管理员可查看所有考试提交",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamSubmissions(
        @Parameter(description = "考试ID", example = "1") @PathVariable examId: Long
    ): Result<List<SubmissionResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以查看考试提交记录", 403)
        }

        return try {
            val submissions = submissionService.getExamSubmissions(examId, user.id ?: 0L, user.role)
            Result.success(submissions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "获取学生的所有成绩",
        description = "学生查看自己的成绩，教师/管理员可查看任何学生成绩",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getUserSubmissions(
        @Parameter(description = "用户ID", example = "1") @PathVariable userId: Long
    ): Result<List<SubmissionResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        // Students can only view their own submissions
        if (user.role == "student" && user.id != userId) {
            return Result.error("您只能查看自己的成绩", 403)
        }

        val submissions = submissionService.getUserSubmissions(userId)
        return Result.success(submissions)
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
