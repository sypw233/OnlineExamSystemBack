package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.CreateNotificationRequest
import ovo.sypw.onlineexamsystemback.dto.response.NotificationResponse
import ovo.sypw.onlineexamsystemback.dto.response.UnreadCountResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.CourseSelectionRepository
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.NotificationService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "系统通知和消息管理")
class NotificationController(
    private val notificationService: NotificationService,
    private val courseSelectionRepository: CourseSelectionRepository,
    private val userRepository: UserRepository
) {

    @GetMapping
    @Operation(
        summary = "获取通知列表",
        description = """
            获取当前用户的通知列表（分页）
            
            ## 功能说明
            - 按创建时间倒序排列
            - 支持分页查询
            - 包含已读和未读通知
            
            ## 参数说明
            - page: 页码（从0开始）
            - size: 每页数量（默认20）
            
            ## 返回数据
            - 通知列表
            - 分页信息
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getNotifications(
        @Parameter(description = "页码", example = "0")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "每页数量", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser user: User
    ): Result<Page<NotificationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        val notifications = notificationService.getUserNotifications(user.requireId(), pageable)
        
        return Result.success(notifications)
    }

    @GetMapping("/unread-count")
    @Operation(
        summary = "获取未读通知数量",
        description = """
            获取当前用户的未读通知数量
            
            ## 用途
            - 导航栏显示未读消息badge
            - 实时更新未读数量
            
            ## 返回
            - count: 未读数量
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getUnreadCount(
        @CurrentUser user: User
    ): Result<UnreadCountResponse> {
        val count = notificationService.getUnreadCount(user.requireId())
        return Result.success(count)
    }

    @PutMapping("/{id}/read")
    @Operation(
        summary = "标记通知为已读",
        description = """
            将指定通知标记为已读
            
            ## 权限
            - 只能标记自己的通知
            
            ## 返回
            - 更新后的通知信息
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun markAsRead(
        @Parameter(description = "通知ID", example = "1")
        @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<NotificationResponse> {
        return try {
            val notification = notificationService.markAsRead(id, user.requireId())
            Result.success(notification, "标记成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "操作失败", 400)
        }
    }

    @PutMapping("/read-all")
    @Operation(
        summary = "全部标记为已读",
        description = """
            将当前用户的所有未读通知标记为已读
            
            ## 返回
            - 标记的通知数量
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun markAllAsRead(
        @CurrentUser user: User
    ): Result<Map<String, Int>> {
        val count = notificationService.markAllAsRead(user.requireId())
        return Result.success(mapOf("count" to count), "已全部标记为已读")
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(
        summary = "发送自定义通知",
        description = """
            管理员发送自定义通知
            
            ## 发送方式（二选一）
            - **按课程发送**: 提供 courseId，该课程下所有选课学生都会收到通知
            - **按用户发送**: 提供 userIds 列表，仅指定用户收到通知
            
            ## 权限
            - 仅管理员可使用此接口
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createNotification(
        @SwaggerRequestBody(description = "通知内容", required = true)
        @Valid @RequestBody request: CreateNotificationRequest,
        @CurrentUser user: User
    ): Result<String> {
        val courseId = request.courseId
        val targetUserIds: List<Long> = if (courseId != null) {
            val enrollments = courseSelectionRepository.findByCourseId(courseId)
            if (enrollments.isEmpty()) {
                return Result.error("该课程下没有学生", 400)
            }
            enrollments.map { it.studentId }
        } else if (!request.userIds.isNullOrEmpty()) {
            request.userIds.orEmpty()
        } else {
            userRepository.findByStatus(1).mapNotNull { it.id }
        }

        if (targetUserIds.isEmpty()) {
            return Result.error("没有可接收通知的用户", 400)
        }

        notificationService.createNotificationsForUsers(
            userIds = targetUserIds,
            type = request.type,
            title = request.title,
            content = request.content,
            relatedId = request.relatedId
        )

        return Result.success("通知发送成功，共 ${targetUserIds.size} 人")
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除通知",
        description = """
            删除指定通知
            
            ## 权限
            - 只能删除自己的通知
            
            ## 注意
            - 删除后无法恢复
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteNotification(
        @Parameter(description = "通知ID", example = "1")
        @PathVariable id: Long,
        @CurrentUser user: User
    ): Result<String> {
        return try {
            notificationService.deleteNotification(id, user.requireId())
            Result.success("删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }
}

private fun User.requireId(): Long =
    id ?: throw IllegalStateException("User entity has not been persisted yet")
