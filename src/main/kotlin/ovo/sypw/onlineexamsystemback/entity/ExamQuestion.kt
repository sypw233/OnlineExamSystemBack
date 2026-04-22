package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "exam_questions")
@IdClass(ExamQuestionId::class)
class ExamQuestion(
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExamQuestion) return false
        return examId == other.examId && questionId == other.questionId
    }

    override fun hashCode(): Int {
        var result = examId.hashCode()
        result = 31 * result + questionId.hashCode()
        return result
    }
}

class ExamQuestionId(
    val examId: Long = 0,
    val questionId: Long = 0
) : Serializable
