package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "创建/更新题目请求")
data class QuestionRequest(
    @field:NotBlank(message = "题目内容不能为空")
    @Schema(
        description = "题目内容",
        example = "What is 1+1?",
        required = true
    )
    val content: String = "",

    @field:NotBlank(message = "题目类型不能为空")
    @field:Pattern(
        regexp = "^(single|multiple|true_false|fill_blank|short_answer)$",
        message = "题目类型必须是: single, multiple, true_false, fill_blank, short_answer"
    )
    @Schema(
        description = "题目类型",
        example = "single",
        required = true,
        allowableValues = ["single", "multiple", "true_false", "fill_blank", "short_answer"],
        implementation = String::class
    )
    val type: String = "",

    @Schema(
        description = "选项（JSON格式数组）- 单选/多选题必填",
        example = "[\"选项A\", \"选项B\", \"选项C\", \"选项D\"]",
        required = false
    )
    val options: String? = null,

    @Schema(
        description = "标准答案",
        example = "选项B",
        required = false
    )
    val answer: String? = null,

    @Schema(
        description = "题目解析",
        example = "这是一道基础数学题...",
        required = false
    )
    val analysis: String? = null,

    @field:NotBlank(message = "难度等级不能为空")
    @field:Pattern(
        regexp = "^(easy|medium|hard)$",
        message = "难度等级必须是: easy, medium, hard"
    )
    @Schema(
        description = "难度等级",
        example = "easy",
        required = true,
        allowableValues = ["easy", "medium", "hard"]
    )
    val difficulty: String = "",

    @field:Size(max = 50, message = "分类长度不能超过50")
    @Schema(
        description = "题目分类/标签",
        example = "数学",
        maxLength = 50,
        required = false
    )
    val category: String? = null
)
