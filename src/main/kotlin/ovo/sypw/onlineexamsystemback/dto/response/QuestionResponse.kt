package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "题目详情响应")
data class QuestionResponse(
    @Schema(description = "题目ID", example = "1")
    val id: Long,
    
    @Schema(description = "题目内容", example = "What is 1+1?")
    val content: String,
    
    @Schema(
        description = "题目类型",
        example = "single",
        allowableValues = ["single", "multiple", "true_false", "fill_blank", "short_answer"]
    )
    val type: String,
    
    @Schema(description = "选项（JSON格式）", example = "[\"1\", \"2\", \"3\", \"4\"]")
    val options: String?,
    
    @Schema(description = "标准答案", example = "2")
    val answer: String?,
    
    @Schema(description = "题目解析", example = "1加1等于2")
    val analysis: String?,
    
    @Schema(
        description = "难度等级",
        example = "easy",
        allowableValues = ["easy", "medium", "hard"]
    )
    val difficulty: String,
    
    @Schema(description = "题目分类", example = "数学")
    val category: String?,
    
    @Schema(description = "创建者ID", example = "1")
    val creatorId: Long,
    
    @Schema(description = "创建者姓名", example = "张老师")
    val creatorName: String,
    
    @Schema(description = "被多少个题库使用", example = "3")
    val bankCount: Long,
    
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    val createTime: LocalDateTime
)
