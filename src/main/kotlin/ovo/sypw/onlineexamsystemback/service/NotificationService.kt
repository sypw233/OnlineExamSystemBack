package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.response.NotificationResponse
import ovo.sypw.onlineexamsystemback.dto.response.UnreadCountResponse
import ovo.sypw.onlineexamsystemback.enums.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface NotificationService {
    /**
     * Create a notification for a user
     */
    fun createNotification(
        userId: Long,
        type: NotificationType,
        title: String,
        content: String?,
        relatedId: Long? = null
    ): NotificationResponse
    
    /**
     * Create notifications for multiple users (batch)
     */
    fun createNotificationsForUsers(
        userIds: List<Long>,
        type: NotificationType,
        title: String,
        content: String?,
        relatedId: Long? = null
    )
    
    /**
     * Get user's notifications with pagination
     */
    fun getUserNotifications(userId: Long, pageable: Pageable): Page<NotificationResponse>
    
    /**
     * Get unread notifications count
     */
    fun getUnreadCount(userId: Long): UnreadCountResponse
    
    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: Long, userId: Long): NotificationResponse
    
    /**
     * Mark all notifications as read
     */
    fun markAllAsRead(userId: Long): Int
    
    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: Long, userId: Long)
    
    /**
     * Send exam published notification to students
     */
    fun sendExamPublishedNotification(examId: Long, examTitle: String, studentIds: List<Long>)
    
    /**
     * Send grade released notification
     */
    fun sendGradeReleasedNotification(examId: Long, examTitle: String, studentId: Long, score: Int)
    
    /**
     * Send exam reminder (scheduled task)
     */
    fun sendExamReminder(examId: Long, examTitle: String, studentIds: List<Long>, hoursBeforeExam: Int)
}
