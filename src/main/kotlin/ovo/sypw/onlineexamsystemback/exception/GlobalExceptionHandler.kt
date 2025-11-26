package ovo.sypw.onlineexamsystemback.exception

import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): Result<Nothing> {
        ex.printStackTrace()
        return Result.error("服务器内部错误: ${ex.message}", 500)
    }
}
