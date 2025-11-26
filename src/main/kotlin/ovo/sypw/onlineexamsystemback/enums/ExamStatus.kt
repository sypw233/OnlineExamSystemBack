package ovo.sypw.onlineexamsystemback.enums

/**
 * 考试状态枚举
 */
enum class ExamStatus(val value: Int, val description: String) {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    ENDED(2, "已结束");

    companion object {
        fun fromValue(value: Int): ExamStatus? {
            return entries.find { it.value == value }
        }
    }
}
