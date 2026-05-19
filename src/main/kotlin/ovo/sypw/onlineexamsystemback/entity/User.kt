package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(name = "nickname", length = 50)
    var nickname: String? = null,

    @Column(name = "real_name", length = 50)
    var realName: String? = null,

    @Column(nullable = false, length = 20)
    var role: String, // admin, teacher, student

    @Column(unique = true, length = 100)
    var email: String? = null,

    @Column(length = 500)
    var avatar: String? = null,

    @Column(nullable = false)
    var status: Int = 1, // 1-Active, 0-Disabled

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
