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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        return toQuestionBankResponse(savedBank, creator.realName ?: creator.username, 0)
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
        val questionCount = questionBankQuestionRepository.countByBankId(id)
        return toQuestionBankResponse(updatedBank, creator.realName ?: creator.username, questionCount)
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
        val questionCount = questionBankQuestionRepository.countByBankId(id)
        return toQuestionBankResponse(questionBank, creator.realName ?: creator.username, questionCount)
    }

    override fun getAllQuestionBanks(pageable: Pageable): Page<QuestionBankResponse> {
        val bankPage = questionBankRepository.findAll(pageable)
        val creatorIds = bankPage.content.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val bankIds = bankPage.content.mapNotNull { it.id }
        val questionCounts = batchFetchQuestionCounts(bankIds)

        return bankPage.map { bank ->
            val creator = creators[bank.creatorId] ?: throw IllegalArgumentException("创建者不存在")
            toQuestionBankResponse(bank, creator.realName ?: creator.username, questionCounts[bank.id] ?: 0)
        }
    }

    override fun getMyQuestionBanks(userId: Long, pageable: Pageable): Page<QuestionBankResponse> {
        val creator = userRepository.findById(userId).orElseThrow {
            throw IllegalArgumentException("用户不存在")
        }
        val bankPage = questionBankRepository.findByCreatorId(userId, pageable)
        val bankIds = bankPage.content.mapNotNull { it.id }
        val questionCounts = batchFetchQuestionCounts(bankIds)

        return bankPage.map { bank ->
            toQuestionBankResponse(bank, creator.realName ?: creator.username, questionCounts[bank.id] ?: 0)
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
        val questionIds = associations.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }
        val creatorIds = questions.values.map { it.creatorId }.toSet()
        val creators = userRepository.findAllById(creatorIds).associateBy { it.id }
        val bankCounts = questionBankQuestionRepository.countByQuestionIdIn(questionIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }

        return associations.mapNotNull { association ->
            questions[association.questionId]?.let { q ->
                val creator = creators[q.creatorId] ?: throw IllegalArgumentException("创建者不存在")
                QuestionResponse(
                    id = q.id ?: 0L,
                    content = q.content,
                    type = q.type,
                    options = q.options,
                    answer = q.answer,
                    analysis = q.analysis,
                    difficulty = q.difficulty,
                    category = q.category,
                    creatorId = q.creatorId,
                    creatorName = creator.realName ?: creator.username,
                    bankCount = bankCounts[q.id] ?: 0,
                    createTime = q.createTime
                )
            }
        }
    }

    private fun toQuestionBankResponse(questionBank: QuestionBank, creatorName: String, questionCount: Long): QuestionBankResponse {
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

    private fun batchFetchQuestionCounts(bankIds: List<Long>): Map<Long, Long> {
        return questionBankQuestionRepository.countByBankIdIn(bankIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }
    }
}
