package ovo.sypw.onlineexamsystemback.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    private val publicPaths = listOf(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/doc.html",
        "/webjars/",
        "/swagger-resources/",
        "/swagger-ui/",
        "/swagger-ui.html",
        "/v3/api-docs/",
        "/favicon.ico"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return publicPaths.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = getJwtFromRequest(request)

            if (token != null && jwtTokenProvider.validateToken(token)) {
                val username = jwtTokenProvider.getUsernameFromToken(token)
                val role = jwtTokenProvider.getRoleFromToken(token)

                val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
                val authentication = UsernamePasswordAuthenticationToken(
                    username, null, authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}
