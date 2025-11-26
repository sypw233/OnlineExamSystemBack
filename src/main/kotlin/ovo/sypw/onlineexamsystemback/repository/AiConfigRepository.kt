package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.AiConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiConfigRepository : JpaRepository<AiConfig, Long> {
    // Find config by key
    fun findByConfigKey(configKey: String): AiConfig?
    
    // Check if config exists
    fun existsByConfigKey(configKey: String): Boolean
}
