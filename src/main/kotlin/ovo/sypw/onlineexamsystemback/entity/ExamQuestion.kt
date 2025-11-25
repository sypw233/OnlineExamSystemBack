package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "exam_questions")
@IdClass(ExamQuestionId::class)
data class ExamQuestion(
    @Id
    @Column(name = "exam_id")
    val examId: Long,

    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Column(nullable = false)
    var score: Int = 1,

    @Column(nullable = false)
    var sequence: Int = 0 // Order of the question in the exam
)

data class ExamQuestionId(
    val examId: Long = 0,
    val questionId: Long = 0
) : Serializable
