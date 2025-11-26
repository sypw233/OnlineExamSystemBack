package ovo.sypw.onlineexamsystemback.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "Bearer Authentication"
        
        return OpenAPI()
            .info(
                Info()
                    .title("在线考试系统 API")
                    .description("在线考试系统后端接口文档")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("开发团队")
                            .email("dev@example.com")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("请输入JWT Token")
                    )
            )
    }
}
