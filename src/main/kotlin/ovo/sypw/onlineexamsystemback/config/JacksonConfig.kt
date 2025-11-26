package ovo.sypw.onlineexamsystemback.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig {

    companion object {
        // 支持多种日期时间格式
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val DATE_TIME_FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        
        // 注册 Java 8 时间模块
        val javaTimeModule = JavaTimeModule()
        
        // 配置 LocalDateTime 序列化器（输出格式）
        javaTimeModule.addSerializer(
            LocalDateTime::class.java,
            LocalDateTimeSerializer(DATE_TIME_FORMATTER)
        )
        
        // 配置 LocalDateTime 反序列化器（接受多种输入格式）
        javaTimeModule.addDeserializer(
            LocalDateTime::class.java,
            FlexibleLocalDateTimeDeserializer()
        )
        
        objectMapper.registerModule(javaTimeModule)
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        
        return objectMapper
    }

    /**
     * 灵活的 LocalDateTime 反序列化器，支持多种格式
     */
    class FlexibleLocalDateTimeDeserializer : LocalDateTimeDeserializer(DATE_TIME_FORMATTER) {
        override fun deserialize(
            p: com.fasterxml.jackson.core.JsonParser,
            ctxt: com.fasterxml.jackson.databind.DeserializationContext
        ): LocalDateTime? {
            val dateString = p.text.trim()
            
            // 尝试多种格式
            return try {
                // 格式1: "yyyy-MM-dd HH:mm:ss"
                LocalDateTime.parse(dateString, DATE_TIME_FORMATTER)
            } catch (e: Exception) {
                try {
                    // 格式2: "yyyy-MM-ddTHH:mm:ss" (ISO)
                    LocalDateTime.parse(dateString, DATE_TIME_FORMATTER_ISO)
                } catch (e2: Exception) {
                    try {
                        // 格式3: "yyyy-MM-dd HH:mm"
                        LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    } catch (e3: Exception) {
                        // 如果都失败，抛出原始异常
                        throw ctxt.weirdStringException(
                            dateString,
                            LocalDateTime::class.java,
                            "无法解析日期时间，支持的格式: yyyy-MM-dd HH:mm:ss 或 yyyy-MM-ddTHH:mm:ss"
                        )
                    }
                }
            }
        }
    }
}
