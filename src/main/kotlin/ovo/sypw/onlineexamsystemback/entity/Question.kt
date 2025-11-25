package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "questions")
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, length = 20)
    var type: String, // single, multiple, true_false, fill_blank, short_answer

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var options: String? = null, // JSON string for options

    @Column(columnDefinition = "TEXT")
    var answer: String? = null,

    @Column(columnDefinition = "TEXT")
    var analysis: String? = null,

    @Column(nullable = false, length = 10)
    var difficulty: String, // easy, medium, hard

    @Column(length = 50)
    var category: String? = null,

    @Column(name = "creator_id", nullable = false)
    val creatorId: Long,

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
)
