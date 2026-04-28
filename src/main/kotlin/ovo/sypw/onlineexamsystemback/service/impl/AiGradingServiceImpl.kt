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
import ovo.sypw.onlineexamsystemback.entity.ExamQuestion
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.AiGradingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Transactional
class AiGradingServiceImpl(
    private val aiConfigRepository: AiConfigRepository,
    private val questionRepository: QuestionRepository,
    private val examSubmissionRepository: ExamSubmissionRepository,
    private val examQuestionRepository: ExamQuestionRepository,
    private val examRepository: ExamRepository,
    private val objectMapper: ObjectMapper
) : AiGradingService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun gradeWithAI(request: AiGradingRequest): AiGradingResponse {
        // Validate question exists
        val question = questionRepository.findById(request.questionId!!).orElseThrow {
            throw IllegalArgumentException("题目不存在")
        }

        // Get AI configurations
        val systemPrompt = getConfigValue("system_prompt")
        val modelName = getConfigValue("model_name")
        val temperature = getConfigValue("temperature").toDoubleOrNull() ?: 0.3
        val maxTokens = getConfigValue("max_tokens").toIntOrNull() ?: 500

        // Build OpenAI request
        val aiRequest = buildOpenAIRequest(
            systemPrompt = systemPrompt,
            questionContent = question.content,
            referenceAnswer = question.answer ?: "",
            studentAnswer = request.studentAnswer!!,
            maxScore = request.maxScore!!,
            modelName = modelName,
            temperature = temperature,
            maxTokens = maxTokens
        )

        // Call OpenAI API
        val aiResponse = callOpenAI(aiRequest)

        // Parse response
        return parseAIResponse(aiResponse, request.questionId!!, request.maxScore!!)
    }

    override fun getAllConfigs(): List<AiConfigResponse> {
        return aiConfigRepository.findAll().map { toConfigResponse(it) }
    }

    override fun getConfig(configKey: String): AiConfigResponse {
        val config = aiConfigRepository.findByConfigKey(configKey)
            ?: throw IllegalArgumentException("配置不存在: $configKey")
        return toConfigResponse(config)
    }

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
        temperature: Double,
        maxTokens: Int
    ): Map<String, Any> {
        val userMessage = """
            题目：$questionContent
            
            参考答案：$referenceAnswer
            
            学生答案：$studentAnswer
            
            题目满分：$maxScore
            
            请评估学生的答案并给出评分建议。
        """.trimIndent()

        return mapOf(
            "model" to modelName,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userMessage)
            ),
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "response_format" to mapOf("type" to "json_object")
        )
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
        val dbValue = aiConfigRepository.findByConfigKey("api_base_url")?.configValue?.takeIf { it.isNotBlank() }
        if (dbValue != null) {
            return dbValue
        }
        return System.getenv("OPENAI_API_BASE_URL")
            ?: "https://api.openai.com/v1"
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

    override fun gradeSubmissionWithAI(
        request: AiBatchGradingRequest,
        userId: Long,
        userRole: String
    ): AiBatchGradingResponse {
        val submissionId = request.submissionId!!

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

        // 3. Get exam questions and filter subjective ones
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
        val concurrency = request.concurrency?.coerceIn(1, 10)
            ?: aiConfigRepository.findByConfigKey("ai_batch_concurrency")?.configValue?.toIntOrNull()?.coerceIn(1, 10)
            ?: 5

        // 6. Parallel AI grading using coroutines with explicit types
        val gradingResults: List<AiGradingDetail> = runBlocking {
            val semaphore = Semaphore(concurrency)
            val deferreds: List<Deferred<AiGradingDetail>> = subjectiveExamQuestions.map { eq: ExamQuestion ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val question = questions[eq.questionId]!!
                        val studentAnswer = studentAnswers[eq.questionId.toString()] ?: ""

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

        // 7. Calculate total suggested score for subjective questions
        val totalSubjectiveSuggestedScore = gradingResults.sumOf { it.suggestedScore }

        // 8. Get existing objective score from submitDetail if available
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
            } catch (_: Exception) {
                // Ignore parse errors
            }
        }

        // 9. Write AI suggested scores to submitDetail (without changing total score or status)
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

        return AiBatchGradingResponse(
            submissionId = submissionId,
            gradedCount = gradingResults.size,
            totalSuggestedScore = totalSubjectiveSuggestedScore + (objectiveScore ?: 0),
            objectiveScore = objectiveScore,
            details = gradingResults
        )
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
