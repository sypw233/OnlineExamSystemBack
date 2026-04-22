package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ai_config")
class AiConfig(
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AiConfig) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
