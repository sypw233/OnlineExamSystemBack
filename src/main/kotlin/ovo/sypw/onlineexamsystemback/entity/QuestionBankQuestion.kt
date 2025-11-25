package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "question_bank_questions")
@IdClass(QuestionBankQuestionId::class)
data class QuestionBankQuestion(
    @Id
    @Column(name = "bank_id")
    val bankId: Long,

    @Id
    @Column(name = "question_id")
    val questionId: Long
)

data class QuestionBankQuestionId(
    val bankId: Long = 0,
    val questionId: Long = 0
) : Serializable
