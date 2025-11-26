package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ovo.sypw.onlineexamsystemback.config.OpenAIProperties
import ovo.sypw.onlineexamsystemback.dto.request.AiConfigRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiGradingRequest
import ovo.sypw.onlineexamsystemback.dto.response.AiConfigResponse
import ovo.sypw.onlineexamsystemback.dto.response.AiGradingResponse
import ovo.sypw.onlineexamsystemback.entity.AiConfig
import ovo.sypw.onlineexamsystemback.repository.AiConfigRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionRepository
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
    private val openAIProperties: OpenAIProperties,
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
        val config = aiConfigRepository.findByConfigKey(request.configKey!!)
            ?: throw IllegalArgumentException("配置不存在: ${request.configKey}")

        config.configValue = request.configValue
        config.updatedBy = userId
        config.updateTime = LocalDateTime.now()

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
        val apiKey = openAIProperties.apiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API Key未配置")
        }

        val jsonBody = objectMapper.writeValueAsString(requestPayload)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val request = Request.Builder()
            .url("${openAIProperties.apiBaseUrl}/chat/completions")
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
