package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.AiConfigRequest
import ovo.sypw.onlineexamsystemback.dto.request.AiGradingRequest
import ovo.sypw.onlineexamsystemback.dto.response.AiConfigResponse
import ovo.sypw.onlineexamsystemback.dto.response.AiGradingResponse

interface AiGradingService {
    /**
     * Use AI to grade a subjective question
     * @param request AI grading request
     * @return AI grading response with suggested score and explanation
     */
    fun gradeWithAI(request: AiGradingRequest): AiGradingResponse
    
    /**
     * Get all AI configurations
     * @return List of AI configurations
     */
    fun getAllConfigs(): List<AiConfigResponse>
    
    /**
     * Get AI configuration by key
     * @param configKey Configuration key
     * @return AI configuration
     */
    fun getConfig(configKey: String): AiConfigResponse
    
    /**
     * Update AI configuration
     * @param request Configuration update request
     * @param userId User ID who is updating
     * @return Updated configuration
     */
    fun updateConfig(request: AiConfigRequest, userId: Long): AiConfigResponse
}
