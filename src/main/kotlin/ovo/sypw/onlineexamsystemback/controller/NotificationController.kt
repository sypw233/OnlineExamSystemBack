package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import ovo.sypw.onlineexamsystemback.dto.response.NotificationResponse
import ovo.sypw.onlineexamsystemback.dto.response.UnreadCountResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.NotificationService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "系统通知和消息管理")
class NotificationController(
    private val notificationService: NotificationService,
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
        @RequestParam(defaultValue = "20") size: Int
    ): Result<Page<NotificationResponse>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        val notifications = notificationService.getUserNotifications(user.id ?: 0L, pageable)
        
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
    fun getUnreadCount(): Result<UnreadCountResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val count = notificationService.getUnreadCount(user.id ?: 0L)
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
        @PathVariable id: Long
    ): Result<NotificationResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            val notification = notificationService.markAsRead(id, user.id ?: 0L)
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
    fun markAllAsRead(): Result<Map<String, Int>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val count = notificationService.markAllAsRead(user.id ?: 0L)
        return Result.success(mapOf("count" to count), "已全部标记为已读")
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
        @PathVariable id: Long
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return try {
            notificationService.deleteNotification(id, user.id ?: 0L)
            Result.success("删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }
}
