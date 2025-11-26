package ovo.sypw.onlineexamsystemback.constants

/**
 * 题目类型常量
 * 用于API文档展示
 */
object QuestionTypeConstants {
    const val SINGLE = "single"           // 单选题
    const val MULTIPLE = "multiple"       // 多选题
    const val TRUE_FALSE = "true_false"   // 判断题
    const val FILL_BLANK = "fill_blank"   // 填空题
    const val SHORT_ANSWER = "short_answer" // 简答题
    
    val ALL_TYPES = listOf(SINGLE, MULTIPLE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER)
    
    const val DESCRIPTION = """
        题目类型说明:
        - single: 单选题 (需提供options)
        - multiple: 多选题 (需提供options)
        - true_false: 判断题
        - fill_blank: 填空题
        - short_answer: 简答题
    """
}

/**
 * 难度等级常量
 */
object DifficultyConstants {
    const val EASY = "easy"       // 简单
    const val MEDIUM = "medium"   // 中等
    const val HARD = "hard"       // 困难
    
    val ALL_LEVELS = listOf(EASY, MEDIUM, HARD)
    
    const val DESCRIPTION = """
        难度等级说明:
        - easy: 简单
        - medium: 中等
        - hard: 困难
    """
}
