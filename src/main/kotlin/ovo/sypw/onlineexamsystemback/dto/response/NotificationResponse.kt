package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import ovo.sypw.onlineexamsystemback.enums.NotificationType
import java.time.LocalDateTime

@Schema(description = "通知响应")
data class NotificationResponse(
    @Schema(description = "通知ID", example = "1")
    val id: Long,
    
    @Schema(description = "通知类型", example = "EXAM_PUBLISHED")
    val type: NotificationType,
    
    @Schema(description = "通知标题", example = "新考试发布")
    val title: String,
    
    @Schema(description = "通知内容", example = "Java期中考试已发布，请及时准备")
    val content: String?,
    
    @Schema(description = "关联ID（考试/课程ID）", example = "1")
    val relatedId: Long?,
    
    @Schema(description = "是否已读", example = "false")
    val isRead: Boolean,
    
    @Schema(description = "创建时间", example = "2024-11-26T10:00:00")
    val createTime: LocalDateTime
)

@Schema(description = "未读通知数量响应")
data class UnreadCountResponse(
    @Schema(description = "未读数量", example = "5")
    val count: Long
)
