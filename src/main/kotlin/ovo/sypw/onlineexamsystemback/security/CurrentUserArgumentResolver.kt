package ovo.sypw.onlineexamsystemback.security

import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Resolves @CurrentUser annotated parameters in controller methods.
 * Injects the currently authenticated User entity.
 * Returns 401 if not authenticated, returns 404 if user not found in DB.
 */
@Component
class CurrentUserArgumentResolver(
    private val userRepository: UserRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.getParameterAnnotation(CurrentUser::class.java) != null
                && parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): User? {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CurrentUserException("未登录", 401)

        val username = authentication.name
        return userRepository.findByUsername(username)
            ?: throw CurrentUserException("用户不存在", 404)
    }
}

/**
 * Exception thrown when @CurrentUser resolution fails.
 * Should be handled by GlobalExceptionHandler to return proper Result response.
 */
class CurrentUserException(
    val errorMessage: String,
    val errorCode: Int
) : RuntimeException(errorMessage)
