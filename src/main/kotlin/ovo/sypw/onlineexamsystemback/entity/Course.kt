package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "courses")
data class Course(
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
)
