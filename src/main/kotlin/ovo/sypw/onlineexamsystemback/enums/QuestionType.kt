package ovo.sypw.onlineexamsystemback.enums

/**
 * 题目类型枚举
 */
enum class QuestionType(val value: String, val description: String) {
    SINGLE("single", "单选题"),
    MULTIPLE("multiple", "多选题"),
    TRUE_FALSE("true_false", "判断题"),
    FILL_BLANK("fill_blank", "填空题"),
    SHORT_ANSWER("short_answer", "简答题");

    companion object {
        fun fromValue(value: String): QuestionType? {
            return entries.find { it.value == value }
        }
    }
}
