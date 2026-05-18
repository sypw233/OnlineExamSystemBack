package ovo.sypw.onlineexamsystemback.config

import ovo.sypw.onlineexamsystemback.security.CurrentUserArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig(
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
    @Value("\${file.upload.path:./uploads/}") private val uploadPath: String
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/doc.html**")
            .addResourceLocations("classpath:/META-INF/resources/")
        
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")

        val uploadLocation = Paths.get(uploadPath)
            .toAbsolutePath()
            .normalize()
            .toUri()
            .toString()
            .let { if (it.endsWith("/")) it else "$it/" }

        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadLocation)
    }
}
