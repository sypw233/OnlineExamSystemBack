package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "question_banks")
class QuestionBank(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "creator_id", nullable = false)
    val creatorId: Long,

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuestionBank) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
