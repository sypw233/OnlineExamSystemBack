package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.ChangePasswordRequest
import ovo.sypw.onlineexamsystemback.dto.request.LoginRequest
import ovo.sypw.onlineexamsystemback.dto.request.RegisterRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserProfileRequest
import ovo.sypw.onlineexamsystemback.dto.response.AuthResponse
import ovo.sypw.onlineexamsystemback.dto.response.UserProfileResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.security.JwtTokenProvider
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

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
    @Operation(
        summary = "用户登录",
        description = """
            通过用户名和密码登录，返回JWT Token
            
            ## 说明
            - 公开端点，无需认证
            - 成功返回 accessToken 和 refreshToken
            - 失败返回 401 错误
        """
    )
    fun login(@Valid @RequestBody loginRequest: LoginRequest): Result<AuthResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )

        SecurityContextHolder.getContext().authentication = authentication

        val user = userRepository.findByUsername(loginRequest.username)
            ?: return Result.error("用户不存在", 404)

        val accessToken = jwtTokenProvider.generateToken(user.username, user.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.username)

        val authResponse = AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            id = user.safeId,
            username = user.username,
            nickname = user.nickname,
            realName = user.realName,
            role = user.role,
            email = user.email,
            avatar = user.avatar
        )

        return Result.success(authResponse, "登录成功")
    }

    @PostMapping("/register")
    @Operation(
        summary = "用户注册",
        description = """
            注册新用户账号（仅支持学生和教师注册）
            
            ## 说明
            - 公开端点，无需认证
            - 禁止注册管理员账号
            - 用户名只能包含字母、数字和下划线
            - 密码长度必须在6-20之间
        """
    )
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): Result<AuthResponse> {
        if (registerRequest.username.isBlank() || registerRequest.password.isBlank()) {
            return Result.error("用户名或者密码为空", 400)
        }
        if (registerRequest.role == "admin") {
            return Result.error("禁止注册管理员账号", 403)
        }
        if (registerRequest.role !in listOf("student", "teacher")) {
            return Result.error("角色只能是学生或教师", 400)
        }
        if (userRepository.existsByUsername(registerRequest.username)) {
            return Result.error("用户名已存在", 400)
        }

        registerRequest.email?.let { email ->
            if (userRepository.existsByEmail(email)) {
                return Result.error("邮箱已被注册", 400)
            }
        }

        val user = User(
            username = registerRequest.username,
            password = passwordEncoder.encode(registerRequest.password)!!,
            realName = registerRequest.realName,
            role = registerRequest.role,
            email = registerRequest.email,
            status = 1
        )

        val savedUser = userRepository.save(user)

        val accessToken = jwtTokenProvider.generateToken(savedUser.username, savedUser.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.username)

        val authResponse = AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            id = savedUser.id ?: 0L,
            username = savedUser.username,
            nickname = savedUser.nickname,
            realName = savedUser.realName,
            role = savedUser.role,
            email = savedUser.email,
            avatar = savedUser.avatar
        )

        return Result.success(authResponse, "注册成功")
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "刷新Token",
        description = """
            使用刷新Token获取新的访问Token
            
            ## 说明
            - 公开端点，无需认证
            - 传入 Bearer 格式的刷新Token
            - 返回新的 accessToken 和 refreshToken
        """
    )
    fun refreshToken(
        @Parameter(description = "Bearer Token", required = true)
        @RequestHeader("Authorization") authorization: String
    ): Result<AuthResponse> {
        val token = authorization.substring(7)

        if (!jwtTokenProvider.validateToken(token)) {
            return Result.error("刷新令牌无效", 401)
        }

        val username = jwtTokenProvider.getUsernameFromToken(token)
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        val newAccessToken = jwtTokenProvider.generateToken(user.username, user.role)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user.username)

        val authResponse = AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            id = user.safeId,
            username = user.username,
            nickname = user.nickname,
            realName = user.realName,
            role = user.role,
            email = user.email,
            avatar = user.avatar
        )

        return Result.success(authResponse, "令牌刷新成功")
    }

    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = "获取当前登录用户的详细信息",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getCurrentUser(): Result<UserProfileResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        return Result.success(
            UserProfileResponse(
                id = user.safeId,
                username = user.username,
                nickname = user.nickname,
                realName = user.realName,
                role = user.role,
                email = user.email,
                avatar = user.avatar
            )
        )
    }

    @PostMapping("/change-password")
    @Operation(
        summary = "修改密码",
        description = "当前登录用户通过旧密码验证后修改密码",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val user = userRepository.findByUsername(authentication.name)
            ?: return Result.error("用户不存在", 404)

        if (!passwordEncoder.matches(request.oldPassword, user.password)) {
            return Result.error("旧密码不正确", 400)
        }

        user.password = passwordEncoder.encode(request.newPassword)!!
        userRepository.save(user)

        return Result.success("密码修改成功")
    }

    @PutMapping("/profile")
    @Operation(
        summary = "修改个人信息",
        description = "当前登录用户修改昵称、邮箱、头像",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateProfile(@Valid @RequestBody request: UserProfileRequest): Result<UserProfileResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val user = userRepository.findByUsername(authentication.name)
            ?: return Result.error("用户不存在", 404)

        request.email?.let { newEmail ->
            val existing = userRepository.findByEmail(newEmail)
            if (existing != null && existing.id != user.id) {
                return Result.error("邮箱已被其他用户使用", 400)
            }
        }

        if (request.nickname != null) user.nickname = request.nickname
        if (request.email != null) user.email = request.email
        if (request.avatar != null) user.avatar = request.avatar

        val saved = userRepository.save(user)

        return Result.success(
            UserProfileResponse(
                id = saved.id ?: 0L,
                username = saved.username,
                nickname = saved.nickname,
                realName = saved.realName,
                role = saved.role,
                email = saved.email,
                avatar = saved.avatar
            ),
            "个人信息修改成功"
        )
    }
}
