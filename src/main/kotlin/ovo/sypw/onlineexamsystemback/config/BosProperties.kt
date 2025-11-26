package ovo.sypw.onlineexamsystemback.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "baidu.bos")
data class BosProperties(
    var accessKeyId: String = "",
    var secretAccessKey: String = "",
    var endpoint: String = "",
    var bucketName: String = ""
)
