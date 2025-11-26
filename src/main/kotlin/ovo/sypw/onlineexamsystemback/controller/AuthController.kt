package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.LoginRequest
import ovo.sypw.onlineexamsystemback.dto.request.RegisterRequest
import ovo.sypw.onlineexamsystemback.dto.response.AuthResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.security.JwtTokenProvider
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import kotlin.math.log

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录，返回JWT Token")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): Result<AuthResponse> {
        // Authenticate user
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )

        SecurityContextHolder.getContext().authentication = authentication

        // Get user info
        val user = userRepository.findByUsername(loginRequest.username)
            ?: return Result.error("User not found", 404)

        // Generate tokens
        val accessToken = jwtTokenProvider.generateToken(user.username, user.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.username)

        val authResponse = AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            username = user.username,
            role = user.role
        )

        return Result.success(authResponse, "Login successful")
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账号（仅支持学生和教师注册）")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): Result<AuthResponse> {
        if(registerRequest.username.isBlank() || registerRequest.password.isBlank()) {
            return Result.error("用户名或者密码为空",400)
        }
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.username)) {
            return Result.error("用户名已存在", 400)
        }

        // Check if email already exists
        registerRequest.email?.let { email ->
            if (userRepository.existsByEmail(email)) {
                return Result.error("邮箱已被注册", 400)
            }
        }
        // Create new user
        val user = User(
            username = registerRequest.username,
            password = passwordEncoder.encode(registerRequest.password)!!,
            realName = registerRequest.realName,
            role = registerRequest.role,
            email = registerRequest.email,
            status = 1
        )

        val savedUser = userRepository.save(user)

        // Generate tokens
        val accessToken = jwtTokenProvider.generateToken(savedUser.username, savedUser.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.username)

        val authResponse = AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            username = savedUser.username,
            role = savedUser.role
        )

        return Result.success(authResponse, "注册成功")
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新Token获取新的访问Token")
    fun refreshToken(
        @Parameter(description = "Bearer Token", required = true)
        @RequestHeader("Authorization") authorization: String
    ): Result<AuthResponse> {
        val token = authorization.substring(7) // Remove "Bearer " prefix

        if (!jwtTokenProvider.validateToken(token)) {
            return Result.error("Invalid refresh token", 401)
        }

        val username = jwtTokenProvider.getUsernameFromToken(token)
        val user = userRepository.findByUsername(username)
            ?: return Result.error("User not found", 404)

        val newAccessToken = jwtTokenProvider.generateToken(user.username, user.role)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user.username)

        val authResponse = AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            username = user.username,
            role = user.role
        )

        return Result.success(authResponse, "Token refreshed")
    }

    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息", 
        description = "获取当前登录用户的详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getCurrentUser(): Result<Map<String, Any>> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("Not authenticated", 401)
        
        val username = authentication.name
        println("username  "+username)
        val user = userRepository.findByUsername(username)
            ?: return Result.error("User not found", 404)

        val userInfo: Map<String, Any> = mapOf(
            "id" to (user.id ?: 0L),
            "username" to user.username,
            "realName" to (user.realName ?: ""),
            "role" to user.role,
            "email" to (user.email ?: "")
        )

        return Result.success(userInfo)
    }
}
