package ovo.sypw.onlineexamsystemback.enums

/**
 * 考试平台限制枚举
 */
enum class ExamPlatform(val value: String, val description: String) {
    DESKTOP("desktop", "仅桌面端"),
    MOBILE("mobile", "仅移动端"),
    BOTH("both", "桌面和移动端");

    companion object {
        fun fromValue(value: String): ExamPlatform? {
            return entries.find { it.value == value }
        }
    }
}
