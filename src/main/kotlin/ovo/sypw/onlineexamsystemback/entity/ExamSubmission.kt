package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "exam_submissions")
data class ExamSubmission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "exam_id", nullable = false)
    val examId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var answers: String? = null, // JSON string for answers

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submit_detail", columnDefinition = "jsonb")
    var submitDetail: String? = null, // JSON string for grading details

    @Column(name = "submit_score")
    var submitScore: Int? = null,

    @Column(nullable = false)
    var status: Int = 0, // 0-In Progress, 1-Submitted, 2-Graded

    @Column(name = "start_time", nullable = false, updatable = false)
    val startTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "submit_time")
    var submitTime: LocalDateTime? = null
)
