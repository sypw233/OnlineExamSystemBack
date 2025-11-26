package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "监考事件记录请求")
data class ProctoringEventRequest(
    @field:NotNull(message = "考试ID不能为空")
    @Schema(
        description = "考试ID（系统会自动找到当前用户的考试提交记录）",
        example = "1",
        required = true
    )
    val examId: Long? = null,
    
    @Schema(
        description = "事件类型",
        example = "tab_switch",
        allowableValues = ["tab_switch", "exit_fullscreen", "blur"],
        required = true
    )
    val eventType: String,
    
    @Schema(
        description = "事件详情",
        example = "切换到其他标签页"
    )
    val detail: String? = null
)
