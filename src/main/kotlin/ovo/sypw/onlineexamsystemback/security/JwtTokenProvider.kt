package ovo.sypw.onlineexamsystemback.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private var jwtExpiration: Long = 0

    @Value("\${jwt.refresh-expiration}")
    private var refreshExpiration: Long = 0

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    /**
     * Generate access token
     */
    fun generateToken(username: String, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Generate refresh token
     */
    fun generateRefreshToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Get username from token
     */
    fun getUsernameFromToken(token: String): String {
        return getClaims(token).subject
    }

    /**
     * Get role from token
     */
    fun getRoleFromToken(token: String): String {
        return getClaims(token)["role"] as String
    }

    /**
     * Validate token
     */
    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
