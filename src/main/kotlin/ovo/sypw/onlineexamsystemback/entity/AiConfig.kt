package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ai_config")
data class AiConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    val configKey: String,
    
    @Column(name = "config_value", columnDefinition = "TEXT")
    var configValue: String? = null,
    
    @Column(length = 255)
    var description: String? = null,
    
    @Column(name = "updated_by")
    var updatedBy: Long? = null,
    
    @Column(name = "update_time")
    var updateTime: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "create_time")
    val createTime: LocalDateTime = LocalDateTime.now()
)
