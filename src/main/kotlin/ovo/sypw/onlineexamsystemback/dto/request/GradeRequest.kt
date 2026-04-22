package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "主观题评分请求")
data class GradeRequest(
    @field:NotEmpty(message = "评分数据不能为空")
    @Schema(
        description = "题目评分（questionId -> score），分数必须在 0-1000 之间",
        example = "{\"4\": 8, \"5\": 15}",
        required = true
    )
    val questionScores: Map<Long, Int> = emptyMap()
)
