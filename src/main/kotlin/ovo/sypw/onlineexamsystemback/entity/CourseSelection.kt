package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "course_selections",
    uniqueConstraints = [UniqueConstraint(columnNames = ["student_id", "course_id"])]
)
data class CourseSelection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "student_id", nullable = false)
    val studentId: Long,

    @Column(name = "course_id", nullable = false)
    val courseId: Long,

    @Column(name = "selection_time", nullable = false, updatable = false)
    val selectionTime: LocalDateTime = LocalDateTime.now()
)
