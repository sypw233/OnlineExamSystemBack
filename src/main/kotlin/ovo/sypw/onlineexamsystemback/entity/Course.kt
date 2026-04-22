package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "courses")
class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "course_name", nullable = false, length = 100)
    var courseName: String,

    @Column(name = "teacher_id", nullable = false)
    val teacherId: Long,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var status: Int = 1, // 1=active, 0=inactive

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Course) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
