package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "瀛︾敓鎴愮哗璁板綍")
data class StudentScoreRecord(
    @Schema(description = "鑰冭瘯ID", example = "1")
    val examId: Long,
    @Schema(description = "鑰冭瘯鏍囬", example = "Java鏈熶腑鑰冭瘯")
    val examTitle: String,
    @Schema(description = "鑰冭瘯鍒嗘暟", example = "85")
    val score: Int?,
    @Schema(description = "鎻愪氦鏃堕棿", example = "2024-10-15T14:30:00")
    val submitTime: LocalDateTime?,
    @Schema(description = "绛斿嵎ID", example = "88")
    val submissionId: Long? = null
)

@Schema(description = "瀛︾敓缁熻鍝嶅簲")
data class StudentStatisticsResponse(
    @Schema(description = "瀛︾敓ID", example = "10")
    val studentId: Long,
    @Schema(description = "瀛︾敓濮撳悕", example = "寮犱笁")
    val studentName: String,
    @Schema(description = "鍙傚姞鑰冭瘯鎬绘暟", example = "8")
    val totalExams: Int,
    @Schema(description = "骞冲潎鎴愮哗", example = "78.5")
    val averageScore: Double?,
    @Schema(description = "鏈€楂樺垎", example = "95")
    val highestScore: Int?,
    @Schema(description = "鏈€浣庡垎", example = "62")
    val lowestScore: Int?,
    @Schema(description = "鎴愮哗璁板綍鍒楄〃")
    val scores: List<StudentScoreRecord>
)
