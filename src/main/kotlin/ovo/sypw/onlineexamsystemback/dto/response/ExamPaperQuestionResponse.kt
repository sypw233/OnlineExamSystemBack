package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "学生考试试卷题目响应（不含答案和解析）")
data class ExamPaperQuestionResponse(
    @Schema(description = "考试ID", example = "1")
    val examId: Long,

    @Schema(description = "题目ID", example = "1")
    val questionId: Long,

    @Schema(description = "题目内容", example = "What is 1+1?")
    val questionContent: String,

    @Schema(description = "题目类型", example = "single")
    val questionType: String,

    @Schema(description = "题目难度", example = "easy")
    val questionDifficulty: String,

    @Schema(description = "选项（JSON格式数组，仅单选/多选题有）", example = "[\"1\", \"2\", \"3\", \"4\"]")
    val options: String?,

    @Schema(description = "题目分值", example = "10")
    val score: Int,

    @Schema(description = "显示顺序", example = "1")
    val sequence: Int
)
