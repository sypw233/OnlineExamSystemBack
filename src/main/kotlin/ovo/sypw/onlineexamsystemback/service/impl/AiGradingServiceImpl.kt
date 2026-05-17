package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ovo.sypw.onlineexamsystemback.dto.request.AiBatchGradingRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiConfigRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiGradingRequest
import ovo.sypw.onlineexamsystemback.dto.response.*
import ovo.sypw.onlineexamsystemback.entity.AiConfig
import ovo.sypw.onlineexamsystemback.entity.Exam
import ovo.sypw.onlineexamsystemback.entity.ExamQuestion
import ovo.sypw.onlineexamsystemback.entity.ExamSubmission
import ovo.sypw.onlineexamsystemback.entity.Question
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.AiGradingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class AiGradingServiceImpl(
    private val aiConfigRepository: AiConfigRepository,
    private val questionRepository: QuestionRepository,
    private val examSubmissionRepository: ExamSubmissionRepository,
    private val examQuestionRepository: ExamQuestionRepository,
    private val examRepository: ExamRepository,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager
) : AiGradingService {

    companion object {
        private val logger = LoggerFactory.getLogger(AiGradingServiceImpl::class.java)

        private data class PresetModel(
            val id: String,
            val modelName: String,
            val baseUrl: String
        )

        private val presetModels = listOf(
            PresetModel("kimi_default", "kimi-k2.6", "https://api.moonshot.ai/v1"),
            PresetModel("openai_gpt_4o_mini", "gpt-4o-mini", "https://api.openai.com/v1"),
            PresetModel("openai_gpt_4_1_mini", "gpt-4.1-mini", "https://api.openai.com/v1"),
            PresetModel("deepseek_chat", "deepseek-chat", "https://api.deepseek.com/v1"),
            PresetModel("qwen_plus", "qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        )
    }

    /**
     * Holds all data loaded from the database for a grading session.
     * Used to separate the read transaction from the AI call phase.
     */
    private data class GradingContext(
        val submission: ExamSubmission,
        val exam: Exam,
        val examQuestions: List<ExamQuestion>,
        val questions: Map<Long?, Question>,
        val studentAnswers: Map<String, String>,
        val concurrency: Int
    )

    private val readOnlyTxTemplate = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    private val writeTxTemplate = TransactionTemplate(transactionManager)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Holds data loaded from DB for single-question AI grading.
     */
    private data class SingleGradingContext(
        val questionContent: String,
        val referenceAnswer: String,
        val systemPrompt: String,
        val modelName: String,
        val temperature: Double?,
        val disableThinking: Boolean,
        val maxTokens: Int
    )

    override fun gradeWithAI(request: AiGradingRequest): AiGradingResponse {
        // Load data in read-only transaction (releases connection after)
        val context = readOnlyTxTemplate.execute {
            val question = questionRepository.findById(request.questionId!!).orElseThrow {
                throw IllegalArgumentException("题目不存在")
            }
            val preset = resolvePresetModel()
            SingleGradingContext(
                questionContent = question.content,
                referenceAnswer = question.answer ?: "",
                systemPrompt = getConfigValue("system_prompt"),
                modelName = preset?.modelName ?: getConfigValue("model_name"),
                temperature = if (preset?.id == "kimi_default") null else getConfigValue("temperature").toDoubleOrNull() ?: 0.3,
                disableThinking = preset?.id == "kimi_default",
                maxTokens = getConfigValue("max_tokens").toIntOrNull() ?: 500
            )
        }!!

        // Build OpenAI request (no DB access)
        val aiRequest = buildOpenAIRequest(
            systemPrompt = context.systemPrompt,
            questionContent = context.questionContent,
            referenceAnswer = context.referenceAnswer,
            studentAnswer = request.studentAnswer!!,
            maxScore = request.maxScore!!,
            modelName = context.modelName,
            temperature = context.temperature,
            disableThinking = context.disableThinking,
            maxTokens = context.maxTokens
        )

        // Call OpenAI API (no DB connection held)
        val aiResponse = callOpenAI(aiRequest)

        // Parse response (no DB access)
        return parseAIResponse(aiResponse, request.questionId!!, request.maxScore!!)
    }

    @Transactional(readOnly = true)
    override fun getAllConfigs(): List<AiConfigResponse> {
        return aiConfigRepository.findAll().map { toConfigResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun getConfig(configKey: String): AiConfigResponse {
        val config = aiConfigRepository.findByConfigKey(configKey)
            ?: throw IllegalArgumentException("配置不存在: $configKey")
        return toConfigResponse(config)
    }

    @Transactional
    override fun updateConfig(request: AiConfigRequest, userId: Long): AiConfigResponse {
        var config = aiConfigRepository.findByConfigKey(request.configKey!!)

        if (config == null) {
            // 配置不存在，创建新配置
            config = AiConfig(
                configKey = request.configKey!!,
                configValue = request.configValue ?: "",
                description = "AI配置项",
                updatedBy = userId
            )
        } else {
            config.configValue = request.configValue ?: ""
            config.updatedBy = userId
            config.updateTime = LocalDateTime.now()
        }

        val savedConfig = aiConfigRepository.save(config)
        return toConfigResponse(savedConfig)
    }

    /**
     * Load all data needed for grading a submission.
     * Must be called within a read-only transaction.
     */
    private fun loadGradingContext(submissionId: Long, userId: Long, userRole: String): GradingContext {
        // 1. Get submission and exam
        val submission = examSubmissionRepository.findById(submissionId).orElseThrow {
            throw IllegalArgumentException("提交记录不存在")
        }
        val exam = examRepository.findById(submission.examId).orElseThrow {
            throw IllegalArgumentException("考试不存在")
        }

        // 2. Permission check
        if (userRole != "admin" && exam.creatorId != userId) {
            throw IllegalArgumentException("您没有权限为此提交评分")
        }

        // 3. Get exam questions and questions
        val examQuestions = examQuestionRepository.findByExamIdOrderBySequence(exam.id!!)
        val questionIds = examQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

        val subjectiveExamQuestions = examQuestions.filter { eq ->
            val question = questions[eq.questionId]
            question?.type in listOf("fill_blank", "short_answer")
        }

        if (subjectiveExamQuestions.isEmpty()) {
            throw IllegalArgumentException("该提交没有主观题需要AI评分")
        }

        // 4. Parse student answers
        val studentAnswers = if (submission.answers != null) {
            objectMapper.readValue(
                submission.answers!!,
                object : com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
            )
        } else {
            emptyMap()
        }

        // 5. Determine concurrency
        val concurrency = aiConfigRepository.findByConfigKey("ai_batch_concurrency")?.configValue?.toIntOrNull()?.coerceIn(1, 10)
            ?: 5

        return GradingContext(
            submission = submission,
            exam = exam,
            examQuestions = subjectiveExamQuestions,
            questions = questions,
            studentAnswers = studentAnswers,
            concurrency = concurrency
        )
    }

    /**
     * Persist AI grading results into the submission's submitDetail.
     * Must be called within a write transaction.
     */
    private fun saveGradingResults(
        submission: ExamSubmission,
        examQuestions: List<ExamQuestion>,
        questions: Map<Long?, Question>,
        gradingResults: List<AiGradingDetail>
    ) {
        val totalSubjectiveSuggestedScore = gradingResults.sumOf { it.suggestedScore }

        // Get existing objective score from submitDetail if available
        var objectiveScore: Int? = null
        val existingScores = mutableMapOf<String, Int>()
        if (submission.submitDetail != null) {
            try {
                val detail = objectMapper.readValue(
                    submission.submitDetail!!,
                    object : com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>() {}
                )
                @Suppress("UNCHECKED_CAST")
                val qScores = detail["questionScores"] as? Map<String, Int>
                qScores?.let { existingScores.putAll(it) }

                // Calculate objective score from existing data
                val allQuestions = examQuestions.mapNotNull { questions[it.questionId] }
                objectiveScore = allQuestions.filter {
                    it.type in listOf("single", "multiple", "true_false")
                }.sumOf { q ->
                    val qid = q.id!!.toString()
                    existingScores[qid] ?: 0
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse submitDetail JSON for objective scoring: ${e.message}")
            }
        }

        // Write AI suggested scores to submitDetail (without changing total score or status)
        for (result in gradingResults) {
            existingScores[result.questionId.toString()] = result.suggestedScore
        }

        val aiDetailsList: List<Map<String, Any>> = gradingResults.map { result ->
            mapOf<String, Any>(
                "questionId" to result.questionId,
                "suggestedScore" to result.suggestedScore,
                "explanation" to result.explanation,
                "strengths" to result.strengths,
                "improvements" to result.improvements
            )
        }

        val newDetail = mutableMapOf<String, Any>(
            "questionScores" to existingScores,
            "aiGraded" to true,
            "aiGradedAt" to LocalDateTime.now().toString(),
            "aiSubjectiveSuggestedScore" to totalSubjectiveSuggestedScore,
            "aiDetails" to aiDetailsList
        )

        if (objectiveScore != null) {
            newDetail["objectiveScore"] = objectiveScore
            newDetail["aiTotalSuggestedScore"] = objectiveScore + totalSubjectiveSuggestedScore
        }

        submission.submitDetail = objectMapper.writeValueAsString(newDetail)
        examSubmissionRepository.save(submission)
    }

    override fun gradeSubmissionWithAI(
        request: AiBatchGradingRequest,
        userId: Long,
        userRole: String
    ): AiBatchGradingResponse {
        val submissionId = request.submissionId!!
        val concurrency = request.concurrency?.coerceIn(1, 10)

        // 1. Load all needed data in a read-only transaction (connection released after)
        val context = readOnlyTxTemplate.execute {
            loadGradingContext(submissionId, userId, userRole)
        }!!
        val effectiveConcurrency = concurrency ?: context.concurrency

        // 2. Parallel AI grading using coroutines (no DB connection held)
        val gradingResults: List<AiGradingDetail> = runBlocking {
            val semaphore = Semaphore(effectiveConcurrency)
            val deferreds: List<Deferred<AiGradingDetail>> = context.examQuestions.map { eq: ExamQuestion ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val question = context.questions[eq.questionId]!!
                        val studentAnswer = context.studentAnswers[eq.questionId.toString()] ?: ""

                        try {
                            val aiRequest = AiGradingRequest(
                                questionId = eq.questionId,
                                studentAnswer = studentAnswer,
                                maxScore = eq.score
                            )
                            val aiResponse = gradeWithAI(aiRequest)
                            AiGradingDetail(
                                questionId = eq.questionId,
                                questionContent = question.content,
                                suggestedScore = aiResponse.suggestedScore,
                                maxScore = eq.score,
                                explanation = aiResponse.explanation,
                                strengths = aiResponse.strengths,
                                improvements = aiResponse.improvements
                            )
                        } catch (e: Exception) {
                            AiGradingDetail(
                                questionId = eq.questionId,
                                questionContent = question.content,
                                suggestedScore = 0,
                                maxScore = eq.score,
                                explanation = "AI评分失败: ${e.message}",
                                strengths = emptyList(),
                                improvements = listOf("请手动评分")
                            )
                        }
                    }
                }
            }
            deferreds.awaitAll()
        }

        // 3. Save results in a write transaction
        writeTxTemplate.execute {
            saveGradingResults(
                submission = context.submission,
                examQuestions = context.examQuestions,
                questions = context.questions,
                gradingResults = gradingResults
            )
        }

        val totalSubjectiveSuggestedScore = gradingResults.sumOf { it.suggestedScore }

        // Recalculate objective score for the response
        var objectiveScore: Int? = null
        if (context.submission.submitDetail != null) {
            try {
                val detail = objectMapper.readValue(
                    context.submission.submitDetail!!,
                    object : com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>() {}
                )
                @Suppress("UNCHECKED_CAST")
                objectiveScore = detail["objectiveScore"] as? Int
            } catch (e: Exception) {
                logger.warn("Failed to parse submitDetail JSON for AI grading response: ${e.message}")
            }
        }

        return AiBatchGradingResponse(
            submissionId = submissionId,
            gradedCount = gradingResults.size,
            totalSuggestedScore = totalSubjectiveSuggestedScore + (objectiveScore ?: 0),
            objectiveScore = objectiveScore,
            details = gradingResults
        )
    }

    /**
     * Get configuration value by key
     */
    private fun getConfigValue(key: String): String {
        return aiConfigRepository.findByConfigKey(key)?.configValue
            ?: throw IllegalArgumentException("配置不存在: $key")
    }

    /**
     * Build OpenAI API request payload
     */
    private fun buildOpenAIRequest(
        systemPrompt: String,
        questionContent: String,
        referenceAnswer: String,
        studentAnswer: String,
        maxScore: Int,
        modelName: String,
        temperature: Double?,
        disableThinking: Boolean,
        maxTokens: Int
    ): Map<String, Any> {
        val userMessage = """
            题目：$questionContent
            
            参考答案：$referenceAnswer
            
            学生答案：$studentAnswer
            
            题目满分：$maxScore
            
            请评估学生的答案并给出评分建议。
        """.trimIndent()

        val payload = mutableMapOf<String, Any>(
            "model" to modelName,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userMessage)
            ),
            "max_tokens" to maxTokens,
            "response_format" to mapOf("type" to "json_object")
        )
        temperature?.let { payload["temperature"] = it }
        if (disableThinking) {
            payload["thinking"] = mapOf("type" to "disabled")
        }
        return payload
    }

    /**
     * Call OpenAI API
     */
    private fun callOpenAI(requestPayload: Map<String, Any>): String {
        val apiKey = getApiKey()
        val apiBaseUrl = getApiBaseUrl()

        val jsonBody = objectMapper.writeValueAsString(requestPayload)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val request = Request.Builder()
            .url("$apiBaseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("OpenAI API调用失败: ${response.code} - ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw RuntimeException("OpenAI API响应为空")

            return responseBody
        }
    }

    /**
     * 获取API Key：优先从数据库读取，为空则从环境变量读取
     */
    private fun getApiKey(): String {
        val dbValue = aiConfigRepository.findByConfigKey("api_key")?.configValue?.takeIf { it.isNotBlank() }
        if (dbValue != null) {
            return dbValue
        }
        return System.getenv("OPENAI_API_KEY")
            ?: throw IllegalStateException("OpenAI API Key未配置（请通过接口配置或设置OPENAI_API_KEY环境变量）")
    }

    /**
     * 获取API Base URL：优先从数据库读取，为空则从环境变量读取
     */
    private fun getApiBaseUrl(): String {
        resolvePresetModel()?.let { return it.baseUrl }
        val dbValue = aiConfigRepository.findByConfigKey("api_base_url")?.configValue?.takeIf { it.isNotBlank() }
        if (dbValue != null) {
            return dbValue
        }
        return System.getenv("OPENAI_API_BASE_URL")
            ?: "https://api.openai.com/v1"
    }

    private fun getModelName(): String {
        resolvePresetModel()?.let { return it.modelName }
        return getConfigValue("model_name")
    }

    private fun resolvePresetModel(): PresetModel? {
        val mode = aiConfigRepository.findByConfigKey("provider_mode")?.configValue?.lowercase()
        if (mode != "preset") return null
        val presetId = aiConfigRepository.findByConfigKey("provider_preset")?.configValue
        return presetModels.firstOrNull { it.id == presetId }
    }

    /**
     * Parse OpenAI API response
     */
    private fun parseAIResponse(responseJson: String, questionId: Long, maxScore: Int): AiGradingResponse {
        try {
            val responseMap = objectMapper.readValue(responseJson, Map::class.java)
            val choices = responseMap["choices"] as? List<*>
                ?: throw RuntimeException("OpenAI响应格式错误：缺少choices")

            val firstChoice = choices.firstOrNull() as? Map<*, *>
                ?: throw RuntimeException("OpenAI响应格式错误：choices为空")

            val message = firstChoice["message"] as? Map<*, *>
                ?: throw RuntimeException("OpenAI响应格式错误：缺少message")

            val content = message["content"] as? String
                ?: throw RuntimeException("OpenAI响应格式错误：缺少content")

            // Parse JSON content
            val aiResult = objectMapper.readValue(content, Map::class.java)

            val suggestedScore = (aiResult["suggestedScore"] as? Number)?.toInt()
                ?: throw RuntimeException("AI响应缺少suggestedScore")

            val explanation = aiResult["explanation"] as? String
                ?: throw RuntimeException("AI响应缺少explanation")

            @Suppress("UNCHECKED_CAST")
            val strengths = (aiResult["strengths"] as? List<String>) ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val improvements = (aiResult["improvements"] as? List<String>) ?: emptyList()

            // Validate score range
            val validatedScore = when {
                suggestedScore < 0 -> 0
                suggestedScore > maxScore -> maxScore
                else -> suggestedScore
            }

            return AiGradingResponse(
                questionId = questionId,
                maxScore = maxScore,
                suggestedScore = validatedScore,
                explanation = explanation,
                strengths = strengths,
                improvements = improvements
            )
        } catch (e: Exception) {
            throw RuntimeException("解析AI响应失败: ${e.message}", e)
        }
    }

    /**
     * Convert AiConfig entity to response
     */
    private fun toConfigResponse(config: AiConfig): AiConfigResponse {
        return AiConfigResponse(
            id = config.id ?: 0L,
            configKey = config.configKey,
            configValue = config.configValue,
            description = config.description
        )
    }
}
