package ovo.sypw.onlineexamsystemback.dto.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val id: Long,
    val username: String,
    val nickname: String?,
    val realName: String?,
    val role: String,
    val email: String?,
    val avatar: String?
)
