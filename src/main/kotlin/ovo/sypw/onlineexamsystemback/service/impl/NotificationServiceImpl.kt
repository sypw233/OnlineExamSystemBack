package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.response.NotificationResponse
import ovo.sypw.onlineexamsystemback.dto.response.UnreadCountResponse
import ovo.sypw.onlineexamsystemback.entity.Notification
import ovo.sypw.onlineexamsystemback.enums.NotificationType
import ovo.sypw.onlineexamsystemback.repository.NotificationRepository
import ovo.sypw.onlineexamsystemback.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository
) : NotificationService {

    override fun createNotification(
        userId: Long,
        type: NotificationType,
        title: String,
        content: String?,
        relatedId: Long?
    ): NotificationResponse {
        val notification = Notification(
            userId = userId,
            type = type,
            title = title,
            content = content,
            relatedId = relatedId
        )
        
        val saved = notificationRepository.save(notification)
        return toResponse(saved)
    }

    override fun createNotificationsForUsers(
        userIds: List<Long>,
        type: NotificationType,
        title: String,
        content: String?,
        relatedId: Long?
    ) {
        val notifications = userIds.map { userId ->
            Notification(
                userId = userId,
                type = type,
                title = title,
                content = content,
                relatedId = relatedId
            )
        }
        
        notificationRepository.saveAll(notifications)
    }

    @Transactional(readOnly = true)
    override fun getUserNotifications(userId: Long, pageable: Pageable): Page<NotificationResponse> {
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable)
            .map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun getUnreadCount(userId: Long): UnreadCountResponse {
        val count = notificationRepository.countByUserIdAndIsRead(userId, false)
        return UnreadCountResponse(count)
    }

    override fun markAsRead(notificationId: Long, userId: Long): NotificationResponse {
        val notification = notificationRepository.findById(notificationId).orElseThrow {
            throw IllegalArgumentException("通知不存在")
        }
        
        if (notification.userId != userId) {
            throw IllegalArgumentException("无权操作此通知")
        }
        
        notification.isRead = true
        val updated = notificationRepository.save(notification)
        return toResponse(updated)
    }

    override fun markAllAsRead(userId: Long): Int {
        return notificationRepository.markAllAsRead(userId)
    }

    override fun deleteNotification(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId).orElseThrow {
            throw IllegalArgumentException("通知不存在")
        }
        
        if (notification.userId != userId) {
            throw IllegalArgumentException("无权删除此通知")
        }
        
        notificationRepository.delete(notification)
    }

    override fun sendExamPublishedNotification(
        examId: Long,
        examTitle: String,
        studentIds: List<Long>
    ) {
        createNotificationsForUsers(
            userIds = studentIds,
            type = NotificationType.EXAM_PUBLISHED,
            title = "新考试发布",
            content = "《$examTitle》已发布，请及时查看并准备",
            relatedId = examId
        )
    }

    override fun sendGradeReleasedNotification(
        examId: Long,
        examTitle: String,
        studentId: Long,
        score: Int
    ) {
        createNotification(
            userId = studentId,
            type = NotificationType.GRADE_RELEASED,
            title = "成绩已发布",
            content = "《$examTitle》的成绩已公布，您的得分：$score",
            relatedId = examId
        )
    }

    override fun sendExamReminder(
        examId: Long,
        examTitle: String,
        studentIds: List<Long>,
        hoursBeforeExam: Int
    ) {
        val timeText = when {
            hoursBeforeExam >= 24 -> "${hoursBeforeExam / 24}天"
            else -> "${hoursBeforeExam}小时"
        }
        
        createNotificationsForUsers(
            userIds = studentIds,
            type = NotificationType.EXAM_REMINDER,
            title = "考试提醒",
            content = "《$examTitle》将在${timeText}后开始，请做好准备",
            relatedId = examId
        )
    }

    private fun toResponse(notification: Notification): NotificationResponse {
        return NotificationResponse(
            id = notification.id ?: 0L,
            type = notification.type,
            title = notification.title,
            content = notification.content,
            relatedId = notification.relatedId,
            isRead = notification.isRead,
            createTime = notification.createTime
        )
    }
}
