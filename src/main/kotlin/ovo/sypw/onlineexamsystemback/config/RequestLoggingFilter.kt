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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, 4096)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val startTime = System.currentTimeMillis()

        log.info("[REQUEST] {} {}", request.method, request.requestURI)
        request.queryString?.let {
            log.info("[REQUEST PARAMS] {}", it)
        }

        filterChain.doFilter(wrappedRequest, wrappedResponse)

        val duration = System.currentTimeMillis() - startTime

        val requestBody = String(wrappedRequest.contentAsByteArray, Charsets.UTF_8)
        if (requestBody.isNotBlank()) {
            log.info("[REQUEST BODY] {}", requestBody)
        }

        val responseBody = String(wrappedResponse.contentAsByteArray, Charsets.UTF_8)
        if (responseBody.isNotBlank()) {
            log.info("[RESPONSE BODY] {}", responseBody)
        }

        log.info("[RESPONSE] {} {} - {}ms", request.method, request.requestURI, duration)

        wrappedResponse.copyBodyToResponse()
    }
}
