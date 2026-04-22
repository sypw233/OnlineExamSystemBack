package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import ovo.sypw.onlineexamsystemback.enums.NotificationType

@Schema(description = "发送自定义通知请求")
data class CreateNotificationRequest(
    @field:NotBlank(message = "通知标题不能为空")
    @Schema(description = "通知标题", example = "系统公告")
    val title: String,

    @Schema(description = "通知内容", example = "系统将于今晚进行维护")
    val content: String? = null,

    @Schema(description = "通知类型", example = "SYSTEM_ANNOUNCEMENT")
    val type: NotificationType = NotificationType.SYSTEM_ANNOUNCEMENT,

    @Schema(description = "关联ID（如考试ID、课程ID）", example = "1")
    val relatedId: Long? = null,

    @Schema(description = "接收用户ID列表（与courseId二选一）")
    val userIds: List<Long>? = null,

    @Schema(description = "按课程批量发送（选课学生都会收到，与userIds二选一）", example = "1")
    val courseId: Long? = null
)
