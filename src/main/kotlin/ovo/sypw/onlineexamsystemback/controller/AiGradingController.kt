package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.AiBatchGradingRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiConfigRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiGradingRequest
import ovo.sypw.onlineexamsystemback.dto.response.AiBatchGradingResponse
import ovo.sypw.onlineexamsystemback.dto.response.AiConfigResponse
import ovo.sypw.onlineexamsystemback.dto.response.AiGradingResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.AiGradingService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai-grading")
@Tag(name = "AI辅助判题", description = "使用AI大模型辅助批改主观题")
class AiGradingController(
    private val aiGradingService: AiGradingService
) {

    @PostMapping("/grade")
    @Operation(
        summary = "AI辅助判题",
        description = """
            使用OpenAI API辅助批改主观题
            
            ## 功能说明
            - 传入题目ID、学生答案和题目满分
            - AI会根据题目内容、参考答案和学生答案进行评分
            - 返回建议分数、评分说明、优点和改进建议
            
            ## 使用场景
            - 教师批改填空题、简答题等主观题时使用
            - AI给出初步评分，教师可以确认或调整
            
            ## 注意事项
            - 需要配置OpenAI API Key
            - 需要网络访问OpenAI服务
            - maxScore必须从ExamQuestion中获取（该题在考试中的分值）
            
            ## 请求示例
            ```json
            {
              "questionId": 1,
              "studentAnswer": "多态是指同一个方法可以有多种形式...",
              "maxScore": 10
            }
            ```
            
            ## 权限
            - 仅教师和管理员可使用
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun gradeWithAI(
        @CurrentUser user: User,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "AI判题请求",
            required = true
        )
        @Valid @RequestBody request: AiGradingRequest
    ): Result<AiGradingResponse> {
        // Only teacher and admin can use AI grading
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以使用AI辅助判题", 403)
        }

        return try {
            val response = aiGradingService.gradeWithAI(request)
            Result.success(response, "AI评分成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "评分失败", 400)
        } catch (e: IllegalStateException) {
            Result.error(e.message ?: "AI服务配置错误", 500)
        } catch (e: RuntimeException) {
            Result.error(e.message ?: "AI服务调用失败", 500)
        }
    }

    @GetMapping("/config")
    @Operation(
        summary = "获取AI配置",
        description = """
            获取所有AI辅助判题的配置信息
            
            ## 配置项
            - system_prompt: 系统提示词（定义评分标准）
            - model_name: 使用的AI模型
            - temperature: 温度参数
            - max_tokens: 最大Token数
            
            ## 权限
            - 仅管理员可查看
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllConfigs(
        @CurrentUser user: User
    ): Result<List<AiConfigResponse>> {
        // Only admin can view configs
        if (user.role != "admin") {
            return Result.error("只有管理员可以查看AI配置", 403)
        }

        return try {
            val configs = aiGradingService.getAllConfigs()
            Result.success(configs)
        } catch (e: Exception) {
            Result.error(e.message ?: "获取配置失败", 500)
        }
    }

    @PutMapping("/config")
    @Operation(
        summary = "更新AI配置",
        description = """
            更新AI辅助判题的配置
            
            ## 可配置项
            - system_prompt: 修改评分标准和行为
            - model_name: 切换AI模型（gpt-4, gpt-3.5-turbo等）
            - temperature: 调整输出随机性（0-2）
            - max_tokens: 限制响应长度
            - ai_batch_concurrency: AI批量评分并行数（1-10）
            
            ## 请求示例
            ```json
            {
              "configKey": "temperature",
              "configValue": "0.5"
            }
            ```
            
            ## 权限
            - 仅管理员可修改
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateConfig(
        @CurrentUser user: User,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "配置更新请求",
            required = true
        )
        @Valid @RequestBody request: AiConfigRequest
    ): Result<AiConfigResponse> {
        // Only admin can update configs
        if (user.role != "admin") {
            return Result.error("只有管理员可以修改AI配置", 403)
        }

        return try {
            val updated = aiGradingService.updateConfig(request, user.safeId)
            Result.success(updated, "配置更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "配置不存在", 404)
        } catch (e: Exception) {
            Result.error(e.message ?: "更新失败", 500)
        }
    }

    @PostMapping("/grade-submission")
    @Operation(
        summary = "AI批量评分",
        description = """
            对一次考试提交中的所有主观题进行AI批量评分
            
            ## 功能说明
            - 自动识别提交中的主观题（填空题、简答题）
            - 并行调用OpenAI API进行评分（并发数可配置，默认5，最大10）
            - 将AI建议分数写入submitDetail，不改变总分和状态（待教师确认）
            - 返回每道题的评分详情和建议总分
            
            ## 使用场景
            - 教师希望对某次提交的所有主观题一键获取AI评分建议
            - 评分结果供教师参考，需教师确认后生效
            
            ## 注意事项
            - 需要配置OpenAI API Key
            - 并发数可通过 ai_batch_concurrency 配置调整
            - 评分结果会覆盖submitDetail中的原有主观题分数（未确认前）
            
            ## 权限
            - 仅教师和管理员可使用
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun gradeSubmissionWithAI(
        @CurrentUser user: User,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "AI批量评分请求",
            required = true
        )
        @Valid @RequestBody request: AiBatchGradingRequest
    ): Result<AiBatchGradingResponse> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以使用AI辅助判题", 403)
        }

        return try {
            val response = aiGradingService.gradeSubmissionWithAI(request, user.safeId, user.role)
            Result.success(response, "AI批量评分完成：共评分 ${response.gradedCount} 道主观题")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "评分失败", 400)
        } catch (e: IllegalStateException) {
            Result.error(e.message ?: "AI服务配置错误", 500)
        } catch (e: RuntimeException) {
            Result.error(e.message ?: "AI服务调用失败", 500)
        }
    }
}
