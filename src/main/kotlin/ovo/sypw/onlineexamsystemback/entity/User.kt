package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(name = "real_name", length = 50)
    var realName: String? = null,

    @Column(nullable = false, length = 20)
    val role: String, // admin, teacher, student

    @Column(unique = true, length = 100)
    var email: String? = null,

    @Column(nullable = false)
    var status: Int = 1, // 1-Active, 0-Disabled

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
)
