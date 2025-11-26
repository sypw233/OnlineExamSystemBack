package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "提交考试答案请求")
data class SubmissionRequest(
    @field:NotNull(message = "考试ID不能为空")
    @Schema(
        description = "考试ID",
        example = "1",
        required = true
    )
    val examId: Long? = null,

    @Schema(
        description = """
            题目答案（questionId -> answer字符串）
            
            答案格式说明:
            - 单选题: "A" 或 "B"
            - 多选题: "A,B,C" (逗号分隔)
            - 判断题: "true" 或 "false"
            - 填空题: "答案内容"
            - 简答题: "详细答案内容"
        """,
        example = """{"1": "A", "2": "B,C", "3": "true", "4": "答案", "5": "这是简答题的答案"}""",
        required = true
    )
    val answers: Map<Long, String> = emptyMap()
)
