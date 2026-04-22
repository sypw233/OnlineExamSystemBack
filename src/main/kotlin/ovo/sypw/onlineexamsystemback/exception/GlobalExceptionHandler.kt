package ovo.sypw.onlineexamsystemback.exception

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import ovo.sypw.onlineexamsystemback.security.CurrentUserException
import ovo.sypw.onlineexamsystemback.util.Result
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentials(ex: BadCredentialsException): Result<Nothing> {
        return Result.error("用户名或密码错误", 401)
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUsernameNotFound(ex: UsernameNotFoundException): Result<Nothing> {
        return Result.error("用户不存在", 401)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: MethodArgumentNotValidException): Result<Nothing> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return Result.error("参数验证失败: $errors", 400)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): Result<Nothing> {
        val cause = ex.cause
        val message = when {
            cause is InvalidDefinitionException || cause is MismatchedInputException -> {
                val path = (cause as? MismatchedInputException)?.path?.joinToString(".") { it.fieldName ?: "[" + it.index + "]" }
                "请求参数格式错误: ${if (path != null) "字段 [$path] " else ""}${cause.originalMessage}"
            }
            else -> "请求体解析失败: ${ex.message}"
        }
        return Result.error(message, 400)
    }

    @ExceptionHandler(CurrentUserException::class)
    fun handleCurrentUserException(ex: CurrentUserException): Result<Nothing> {
        val status = when (ex.errorCode) {
            401 -> HttpStatus.UNAUTHORIZED
            404 -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        return Result.error(ex.errorMessage, ex.errorCode)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Result<Nothing> {
        return Result.error(ex.message ?: "请求参数错误", 400)
    }

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalState(ex: IllegalStateException): Result<Nothing> {
        return Result.error(ex.message ?: "状态异常", 400)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): Result<Nothing> {
        logger.error("服务器内部错误: ${ex.message}", ex)
        return Result.error("服务器内部错误: ${ex.message}", 500)
    }
}
