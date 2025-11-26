package ovo.sypw.onlineexamsystemback.dto.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val username: String,
    val role: String
)
