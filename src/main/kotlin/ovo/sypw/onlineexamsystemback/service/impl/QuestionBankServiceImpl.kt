package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.QuestionBankRequest
import ovo.sypw.onlineexamsystemback.dto.response.QuestionBankResponse
import ovo.sypw.onlineexamsystemback.dto.response.QuestionResponse
import ovo.sypw.onlineexamsystemback.entity.QuestionBank
import ovo.sypw.onlineexamsystemback.entity.QuestionBankQuestion
import ovo.sypw.onlineexamsystemback.repository.QuestionBankQuestionRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionBankRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionRepository
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.QuestionBankService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuestionBankServiceImpl(
    private val questionBankRepository: QuestionBankRepository,
    private val questionRepository: QuestionRepository,
    private val questionBankQuestionRepository: QuestionBankQuestionRepository,
    private val userRepository: UserRepository
) : QuestionBankService {

    override fun createQuestionBank(request: QuestionBankRequest, creatorId: Long): QuestionBankResponse {
        val creator = userRepository.findById(creatorId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        if (creator.role != "teacher" && creator.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以创建题库")
        }

        val questionBank = QuestionBank(
            name = request.name,
            description = request.description,
            creatorId = creatorId
        )

        val savedBank = questionBankRepository.save(questionBank)
        return toQuestionBankResponse(savedBank, creator.realName ?: creator.username)
    }

    override fun updateQuestionBank(
        id: Long,
        request: QuestionBankRequest,
        userId: Long,
        userRole: String
    ): QuestionBankResponse {
        val questionBank = questionBankRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        // Check permission
        if (userRole != "admin" && questionBank.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此题库")
        }

        questionBank.name = request.name
        questionBank.description = request.description

        val updatedBank = questionBankRepository.save(questionBank)
        val creator = userRepository.findById(questionBank.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toQuestionBankResponse(updatedBank, creator.realName ?: creator.username)
    }

    override fun deleteQuestionBank(id: Long, userId: Long, userRole: String) {
        val questionBank = questionBankRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        // Check permission
        if (userRole != "admin" && questionBank.creatorId != userId) {
            throw IllegalArgumentException("您没有权限删除此题库")
        }

        // Optional: Allow deleting even with questions, or enforce empty bank
        val questionCount = questionBankQuestionRepository.countByBankId(id)
        if (questionCount > 0) {
            throw IllegalArgumentException("题库中还有 $questionCount 道题目，请先清空题库")
        }

        questionBankRepository.delete(questionBank)
    }

    override fun getQuestionBankById(id: Long): QuestionBankResponse {
        val questionBank = questionBankRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        val creator = userRepository.findById(questionBank.creatorId).orElseThrow {
            throw IllegalArgumentException("创建者不存在")
        }

        return toQuestionBankResponse(questionBank, creator.realName ?: creator.username)
    }

    override fun getAllQuestionBanks(): List<QuestionBankResponse> {
        val questionBanks = questionBankRepository.findAll()
        return questionBanks.map { bank ->
            val creator = userRepository.findById(bank.creatorId).orElseThrow {
                throw IllegalArgumentException("创建者不存在")
            }
            toQuestionBankResponse(bank, creator.realName ?: creator.username)
        }
    }

    override fun getMyQuestionBanks(userId: Long): List<QuestionBankResponse> {
        val questionBanks = questionBankRepository.findByCreatorId(userId)
        val creator = userRepository.findById(userId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }

        return questionBanks.map { bank ->
            toQuestionBankResponse(bank, creator.realName ?: creator.username)
        }
    }

    override fun addQuestionToBank(bankId: Long, questionId: Long, userId: Long, userRole: String) {
        // Verify bank exists
        val questionBank = questionBankRepository.findById(bankId).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        // Check permission
        if (userRole != "admin" && questionBank.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此题库")
        }

        // Verify question exists
        questionRepository.findById(questionId).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Check if already in bank
        if (questionBankQuestionRepository.existsByBankIdAndQuestionId(bankId, questionId)) {
            throw IllegalArgumentException("该题目已在题库中")
        }

        val association = QuestionBankQuestion(
            bankId = bankId,
            questionId = questionId
        )

        questionBankQuestionRepository.save(association)
    }

    override fun removeQuestionFromBank(bankId: Long, questionId: Long, userId: Long, userRole: String) {
        // Verify bank exists
        val questionBank = questionBankRepository.findById(bankId).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        // Check permission
        if (userRole != "admin" && questionBank.creatorId != userId) {
            throw IllegalArgumentException("您没有权限修改此题库")
        }

        // Check if question is in bank
        if (!questionBankQuestionRepository.existsByBankIdAndQuestionId(bankId, questionId)) {
            throw IllegalArgumentException("该题目不在此题库中")
        }

        questionBankQuestionRepository.deleteByBankIdAndQuestionId(bankId, questionId)
    }

    override fun getQuestionsInBank(bankId: Long): List<QuestionResponse> {
        // Verify bank exists
        questionBankRepository.findById(bankId).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        val associations = questionBankQuestionRepository.findByBankId(bankId)
        return associations.mapNotNull { association ->
            val question = questionRepository.findById(association.questionId).orElse(null)
            question?.let {
                val creator = userRepository.findById(it.creatorId).orElseThrow {
                    throw IllegalArgumentException("创建者不存在")
                }
                val bankCount = questionBankQuestionRepository.countByQuestionId(it.id ?: 0L)
                QuestionResponse(
                    id = it.id ?: 0L,
                    content = it.content,
                    type = it.type,
                    options = it.options,
                    answer = it.answer,
                    analysis = it.analysis,
                    difficulty = it.difficulty,
                    category = it.category,
                    creatorId = it.creatorId,
                    creatorName = creator.realName ?: creator.username,
                    bankCount = bankCount,
                    createTime = it.createTime
                )
            }
        }
    }

    private fun toQuestionBankResponse(questionBank: QuestionBank, creatorName: String): QuestionBankResponse {
        val questionCount = questionBankQuestionRepository.countByBankId(questionBank.id ?: 0L)
        return QuestionBankResponse(
            id = questionBank.id ?: 0L,
            name = questionBank.name,
            description = questionBank.description,
            creatorId = questionBank.creatorId,
            creatorName = creatorName,
            questionCount = questionCount,
            createTime = questionBank.createTime
        )
    }
}
