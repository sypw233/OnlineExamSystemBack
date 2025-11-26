package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "题库详情响应")
data class QuestionBankResponse(
    @Schema(description = "题库ID", example = "1")
    val id: Long,
    
    @Schema(description = "题库名称", example = "Java基础题库")
    val name: String,
    
    @Schema(description = "题库描述", example = "包含Java基础知识的所有题目")
    val description: String?,
    
    @Schema(description = "创建者ID", example = "1")
    val creatorId: Long,
    
    @Schema(description = "创建者姓名", example = "张老师")
    val creatorName: String,
    
    @Schema(description = "题库中题目数量", example = "50")
    val questionCount: Long,
    
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    val createTime: LocalDateTime
)
