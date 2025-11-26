package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    
    // Find notifications by user ID with pagination
    fun findByUserIdOrderByCreateTimeDesc(userId: Long, pageable: Pageable): Page<Notification>
    
    // Count unread notifications for a user
    fun countByUserIdAndIsRead(userId: Long, isRead: Boolean): Long
    
    // Find unread notifications for a user
    fun findByUserIdAndIsReadOrderByCreateTimeDesc(
        userId: Long, 
        isRead: Boolean, 
        pageable: Pageable
    ): Page<Notification>
    
    // Mark all notifications as read for a user
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    fun markAllAsRead(userId: Long): Int
    
    // Delete old read notifications (cleanup task)
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true AND n.createTime < :beforeDate")
    fun deleteOldReadNotifications(userId: Long, beforeDate: java.time.LocalDateTime): Int
}
