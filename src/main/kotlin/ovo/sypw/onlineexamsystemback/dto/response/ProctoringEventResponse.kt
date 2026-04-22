package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "监考事件记录响应")
data class ProctoringEventResponse(
    @Schema(description = "是否已记录", example = "true")
    val recorded: Boolean,

    @Schema(description = "是否已自动提交考试", example = "false")
    val autoSubmitted: Boolean
)
