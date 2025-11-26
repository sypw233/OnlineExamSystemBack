package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import ovo.sypw.onlineexamsystemback.enums.NotificationType
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: NotificationType,
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val content: String?,
    
    @Column(name = "related_id")
    val relatedId: Long? = null,
    
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
    
    @Column(name = "create_time", nullable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
)
