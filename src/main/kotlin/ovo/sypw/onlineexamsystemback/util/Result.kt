package ovo.sypw.onlineexamsystemback.util

data class Result<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T? = null, message: String = "Success"): Result<T> {
            return Result(200, message, data)
        }

        fun <T> error(message: String, code: Int = 500): Result<T> {
            return Result(code, message, null)
        }
    }
}
