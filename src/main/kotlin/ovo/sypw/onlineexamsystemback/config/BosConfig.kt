package ovo.sypw.onlineexamsystemback.config

import com.baidubce.auth.DefaultBceCredentials
import com.baidubce.services.bos.BosClient
import com.baidubce.services.bos.BosClientConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BosConfig(
    private val bosProperties: BosProperties
) {

    @Bean
    fun bosClient(): BosClient {
        val config = BosClientConfiguration()
        config.credentials = DefaultBceCredentials(
            bosProperties.accessKeyId,
            bosProperties.secretAccessKey
        )
        config.endpoint = bosProperties.endpoint
        
        return BosClient(config)
    }
}
