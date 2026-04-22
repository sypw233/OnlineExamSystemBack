package ovo.sypw.onlineexamsystemback.security

import io.swagger.v3.oas.annotations.Parameter

/**
 * Annotation to inject the currently authenticated User entity into a controller method parameter.
 *
 * Usage:
 * ```kotlin
 * fun someMethod(@CurrentUser user: User): Result<...> {
 *     // user is already resolved and authenticated
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(hidden = true)
annotation class CurrentUser
