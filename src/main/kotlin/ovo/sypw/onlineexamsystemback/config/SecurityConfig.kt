package ovo.sypw.onlineexamsystemback.config

import ovo.sypw.onlineexamsystemback.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint { _, response, authException ->
                        response.status = 401
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            """{"code":401,"message":"Unauthorized: ${authException.message}","data":null}"""
                        )
                    }
                    .accessDeniedHandler { _, response, accessDeniedException ->
                        response.status = 403
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write(
                            """{"code":403,"message":"Access Denied: ${accessDeniedException.message}","data":null}"""
                        )
                    }
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Authentication endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    // Knife4j and Swagger endpoints
                    .requestMatchers(
                        "/doc.html",
                        "/doc.html/**",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }
}
