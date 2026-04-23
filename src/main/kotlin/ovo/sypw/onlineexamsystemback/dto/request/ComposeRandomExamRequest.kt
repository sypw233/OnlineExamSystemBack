package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

@Schema(description = "智能随机组卷请求")
data class ComposeRandomExamRequest(
    @field:NotNull(message = "题库ID不能为空")
    @Schema(description = "选题的题库ID", example = "1", required = true)
    val bankId: Long? = null,

    @field:Min(value = 1, message = "期望总分至少为1")
    @Schema(description = "期望总分（可选，若填写则校验实际总分是否匹配）", example = "100", required = false)
    val expectedTotalScore: Int? = null,

    @field:Valid
    @Schema(description = "组卷规则列表", required = true)
    val sections: List<SectionRule> = emptyList(),

    @Schema(description = "组卷选项", required = false)
    val options: ComposeOptions? = null
)

@Schema(description = "单条组卷规则（按题型）")
data class SectionRule(
    @field:NotBlank(message = "题型不能为空")
    @field:Pattern(
        regexp = "^(single|multiple|true_false|fill_blank|short_answer)$",
        message = "题型必须是: single, multiple, true_false, fill_blank, short_answer"
    )
    @Schema(
        description = "题目类型",
        example = "single",
        allowableValues = ["single", "multiple", "true_false", "fill_blank", "short_answer"]
    )
    val type: String = "",

    @field:Min(value = 1, message = "题目数量至少为1")
    @Schema(description = "该题型抽取的题目数量", example = "10")
    val count: Int = 0,

    @field:Min(value = 1, message = "每题分值至少为1")
    @Schema(description = "该题型每题分值", example = "2")
    val scorePerQuestion: Int = 0,

    @Schema(
        description = "难度分配（绝对数量）。key: easy/medium/hard，value: 该难度的题目数量。所有value之和必须等于count。不提供则不限难度随机抽取。",
        example = """{"easy": 4, "medium": 4, "hard": 2}"""
    )
    val difficultyDistribution: Map<String, Int>? = null
)

@Schema(description = "组卷选项")
data class ComposeOptions(
    @Schema(description = "是否打乱题目顺序", example = "true", defaultValue = "true")
    val shuffleQuestions: Boolean = true,

    @Schema(description = "宽松模式：某难度题目不足时，自动从同题型其他难度补齐", example = "false", defaultValue = "false")
    val lenientMode: Boolean = false
)
