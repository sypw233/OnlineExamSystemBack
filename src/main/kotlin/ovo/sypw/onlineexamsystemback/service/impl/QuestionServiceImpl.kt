package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.QuestionRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.entity.Question
import ovo.sypw.onlineexamsystemback.repository.QuestionBankQuestionRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionRepository
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.QuestionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuestionServiceImpl(
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val questionBankQuestionRepository: QuestionBankQuestionRepository
) : QuestionService {

    override fun createQuestion(questionRequest: QuestionRequest, creatorId: Long): QuestionResponse {
        val creator = userRepository.findById(creatorId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        // Validate teacher/admin role
        if (creator.role != "teacher" && creator.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以创建题目")
        }

        // Validate options for choice questions
        if ((questionRequest.type == "single" || questionRequest.type == "multiple") 
            && questionRequest.options.isNullOrBlank()) {
            throw IllegalArgumentException("单选和多选题必须提供选项")
        }

        val question = Question(
            content = questionRequest.content,
            type = questionRequest.type,
            options = questionRequest.options,
            answer = questionRequest.answer,
            analysis = questionRequest.analysis,
            difficulty = questionRequest.difficulty,
            category = questionRequest.category,
            creatorId = creatorId
        )

        val savedQuestion = questionRepository.save(question)
        return toQuestionResponse(savedQuestion, creator.realName ?: creator.username)
    }

    override fun updateQuestion(
        id: Long,
        questionRequest: QuestionRequest,
        userId: Long,
        userRole: String
    ): QuestionResponse {
        val question = questionRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Check permission: admin can update any, teacher can only update their own
        if (userRole != "admin" && question.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此题目")
        }

        // Validate options if needed
        if ((questionRequest.type == "single" || questionRequest.type == "multiple") 
            && questionRequest.options.isNullOrBlank()) {
            throw IllegalArgumentException("单选和多选题必须提供选项")
        }

        question.content = questionRequest.content
        question.type = questionRequest.type
        question.options = questionRequest.options
        question.answer = questionRequest.answer
        question.analysis = questionRequest.analysis
        question.difficulty = questionRequest.difficulty
        question.category = questionRequest.category

        val updatedQuestion = questionRepository.save(question)
        val creator = userRepository.findById(question.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toQuestionResponse(updatedQuestion, creator.realName ?: creator.username)
    }

    override fun deleteQuestion(id: Long, userId: Long, userRole: String) {
        val question = questionRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Check permission
        if (userRole != "admin" && question.creatorId != userId) {
            throw IllegalArgumentException("您没有权限删除此题目")
        }

        // Check if question is used in any banks
        val bankCount = questionBankQuestionRepository.countByQuestionId(id)
        if (bankCount > 0) {
            throw IllegalArgumentException("该题目已被 $bankCount 个题库使用，无法删除")
        }

        questionRepository.delete(question)
    }

    override fun getQuestionById(id: Long): QuestionResponse {
        val question = questionRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        val creator = userRepository.findById(question.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toQuestionResponse(question, creator.realName ?: creator.username)
    }

    override fun getAllQuestions(): List<QuestionResponse> {
        val questions = questionRepository.findAll()
        return questions.map { question ->
            val creator = userRepository.findById(question.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toQuestionResponse(question, creator.realName ?: creator.username)
        }
    }

    override fun getMyQuestions(userId: Long): List<QuestionResponse> {
        val questions = questionRepository.findByCreatorId(userId)
        val creator = userRepository.findById(userId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        return questions.map { question ->
            toQuestionResponse(question, creator.realName ?: creator.username)
        }
    }

    override fun getQuestionsByType(type: String): List<QuestionResponse> {
        val questions = questionRepository.findByType(type)
        return questions.map { question ->
            val creator = userRepository.findById(question.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toQuestionResponse(question, creator.realName ?: creator.username)
        }
    }

    override fun getQuestionsByDifficulty(difficulty: String): List<QuestionResponse> {
        val questions = questionRepository.findByDifficulty(difficulty)
        return questions.map { question ->
            val creator = userRepository.findById(question.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toQuestionResponse(question, creator.realName ?: creator.username)
        }
    }

    override fun getQuestionsByCategory(category: String): List<QuestionResponse> {
        val questions = questionRepository.findByCategory(category)
        return questions.map { question ->
            val creator = userRepository.findById(question.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toQuestionResponse(question, creator.realName ?: creator.username)
        }
    }

    private fun toQuestionResponse(question: Question, creatorName: String): QuestionResponse {
        val bankCount = questionBankQuestionRepository.countByQuestionId(question.id ?: 0L)
        return QuestionResponse(
            id = question.id ?: 0L,
            content = question.content,
            type = question.type,
            options = question.options,
            answer = question.answer,
            analysis = question.analysis,
            difficulty = question.difficulty,
            category = question.category,
            creatorId = question.creatorId,
            creatorName = creatorName,
            bankCount = bankCount,
            createTime = question.createTime
        )
    }
}
