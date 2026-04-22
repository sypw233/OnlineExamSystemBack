package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "question_bank_questions")
@IdClass(QuestionBankQuestionId::class)
class QuestionBankQuestion(
    @Id
    @Column(name = "bank_id")
    val bankId: Long,

    @Id
    @Column(name = "question_id")
    val questionId: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuestionBankQuestion) return false
        return bankId == other.bankId && questionId == other.questionId
    }

    override fun hashCode(): Int {
        var result = bankId.hashCode()
        result = 31 * result + questionId.hashCode()
        return result
    }
}

class QuestionBankQuestionId(
    val bankId: Long = 0,
    val questionId: Long = 0
) : Serializable
