package ovo.sypw.onlineexamsystemback.enums

/**
 * 题目难度等级枚举
 */
enum class DifficultyLevel(val value: String, val description: String) {
    EASY("easy", "简单"),
    MEDIUM("medium", "中等"),
    HARD("hard", "困难");

    companion object {
        fun fromValue(value: String): DifficultyLevel? {
            return entries.find { it.value == value }
        }
    }
}
