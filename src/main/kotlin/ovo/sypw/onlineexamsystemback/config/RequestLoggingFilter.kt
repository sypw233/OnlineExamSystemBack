package ovo.sypw.onlineexamsystemback.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    private val sensitivePathPrefixes = listOf("/api/auth/")
    private val sensitiveFieldPattern = Regex(
        """("(?:password|token|secret|accessToken|refreshToken|oldPassword|newPassword)"\s*:\s*")([^"]*?)"""",
        RegexOption.IGNORE_CASE
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, 4096)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val startTime = System.currentTimeMillis()
        val uri = request.requestURI
        val isSensitivePath = sensitivePathPrefixes.any { uri.startsWith(it) }

        log.info("[REQUEST] {} {}", request.method, uri)
        request.queryString?.let {
            log.info("[REQUEST PARAMS] {}", it)
        }

        filterChain.doFilter(wrappedRequest, wrappedResponse)

        val duration = System.currentTimeMillis() - startTime

        if (!isSensitivePath) {
            val requestBody = String(wrappedRequest.contentAsByteArray, Charsets.UTF_8)
            if (requestBody.isNotBlank()) {
                log.info("[REQUEST BODY] {}", requestBody)
            }
        }

        val responseBody = String(wrappedResponse.contentAsByteArray, Charsets.UTF_8)
        if (responseBody.isNotBlank()) {
            val redacted = redactSensitiveFields(responseBody)
            log.info("[RESPONSE BODY] {}", redacted)
        }

        log.info("[RESPONSE] {} {} - {}ms", request.method, uri, duration)

        wrappedResponse.copyBodyToResponse()
    }

    private fun redactSensitiveFields(body: String): String {
        return sensitiveFieldPattern.replace(body) { matchResult ->
            "${matchResult.groupValues[1]}******\""
        }
    }
}
