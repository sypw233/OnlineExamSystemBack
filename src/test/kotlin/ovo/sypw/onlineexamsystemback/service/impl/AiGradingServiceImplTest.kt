package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import ovo.sypw.onlineexamsystemback.entity.AiConfig
import ovo.sypw.onlineexamsystemback.repository.AiConfigRepository

@ExtendWith(MockitoExtension::class)
class AiGradingServiceImplTest {

    @Mock
    private lateinit var aiConfigRepository: AiConfigRepository

    private lateinit var objectMapper: ObjectMapper

    private lateinit var aiGradingService: AiGradingServiceImpl

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        aiGradingService = AiGradingServiceImpl(
            aiConfigRepository = aiConfigRepository,
            questionRepository = mock(),
            examSubmissionRepository = mock(),
            examQuestionRepository = mock(),
            examRepository = mock(),
            objectMapper = objectMapper
        )
    }

    @Test
    fun `test getApiKey from database`() {
        // Given
        val apiKey = "sk-test-key-from-db"
        whenever(aiConfigRepository.findByConfigKey("api_key"))
            .thenReturn(AiConfig(configKey = "api_key", configValue = apiKey))

        // When & Then - 使用反射调用私有方法
        val method = AiGradingServiceImpl::class.java.getDeclaredMethod("getApiKey")
        method.isAccessible = true
        val result = method.invoke(aiGradingService) as String

        assert(result == apiKey)
    }

    @Test
    fun `test getApiKey fallback to env`() {
        // Given
        whenever(aiConfigRepository.findByConfigKey("api_key"))
            .thenReturn(AiConfig(configKey = "api_key", configValue = ""))

        // 设置环境变量
        val envKey = "test-env-key"
        setEnv("OPENAI_API_KEY", envKey)

        // When & Then
        val method = AiGradingServiceImpl::class.java.getDeclaredMethod("getApiKey")
        method.isAccessible = true
        val result = method.invoke(aiGradingService) as String

        assert(result == envKey)

        // Cleanup
        setEnv("OPENAI_API_KEY", null)
    }

    @Test
    fun `test getApiBaseUrl from database`() {
        // Given
        val baseUrl = "https://api.moonshot.ai/v1"
        whenever(aiConfigRepository.findByConfigKey("api_base_url"))
            .thenReturn(AiConfig(configKey = "api_base_url", configValue = baseUrl))

        // When & Then
        val method = AiGradingServiceImpl::class.java.getDeclaredMethod("getApiBaseUrl")
        method.isAccessible = true
        val result = method.invoke(aiGradingService) as String

        assert(result == baseUrl)
    }

    @Test
    fun `test getApiBaseUrl fallback to env`() {
        // Given
        whenever(aiConfigRepository.findByConfigKey("api_base_url"))
            .thenReturn(AiConfig(configKey = "api_base_url", configValue = ""))

        val envUrl = "https://api.openai.com/v1"
        setEnv("OPENAI_API_BASE_URL", envUrl)

        // When & Then
        val method = AiGradingServiceImpl::class.java.getDeclaredMethod("getApiBaseUrl")
        method.isAccessible = true
        val result = method.invoke(aiGradingService) as String

        assert(result == envUrl)

        // Cleanup
        setEnv("OPENAI_API_BASE_URL", null)
    }

    @Test
    fun `test getApiBaseUrl default value`() {
        // Given
        whenever(aiConfigRepository.findByConfigKey("api_base_url"))
            .thenReturn(AiConfig(configKey = "api_base_url", configValue = ""))

        // 确保环境变量也不存在
        setEnv("OPENAI_API_BASE_URL", null)

        // When & Then
        val method = AiGradingServiceImpl::class.java.getDeclaredMethod("getApiBaseUrl")
        method.isAccessible = true
        val result = method.invoke(aiGradingService) as String

        assert(result == "https://api.openai.com/v1")
    }

    // Helper method to set environment variables
    private fun setEnv(key: String, value: String?) {
        try {
            val envClass = Class.forName("java.lang.ProcessEnvironment")
            val theEnvironmentField = envClass.getDeclaredField("theEnvironment")
            theEnvironmentField.isAccessible = true
            val env = theEnvironmentField.get(null) as MutableMap<String, String>
            if (value != null) {
                env[key] = value
            } else {
                env.remove(key)
            }
        } catch (e: Exception) {
            // Fallback for different JDK implementations
            try {
                val envClass = Class.forName("java.lang.ProcessEnvironment")
                val theUnmodifiableEnvironmentField = envClass.getDeclaredField("theUnmodifiableEnvironment")
                theUnmodifiableEnvironmentField.isAccessible = true
                val env = theUnmodifiableEnvironmentField.get(null) as MutableMap<String, String>
                if (value != null) {
                    env[key] = value
                } else {
                    env.remove(key)
                }
            } catch (e2: Exception) {
                // Ignore if can't set env
            }
        }
    }

    // Helper to create mocks
    private inline fun <reified T> mock(): T = org.mockito.Mockito.mock(T::class.java)
}
