package ovo.sypw.onlineexamsystemback.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "openai")
data class OpenAIProperties(
    var apiKey: String = "",
    var apiBaseUrl: String = "https://api.openai.com/v1"
)
