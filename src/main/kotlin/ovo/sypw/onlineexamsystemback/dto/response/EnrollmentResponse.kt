package ovo.sypw.onlineexamsystemback.dto.response

import java.time.LocalDateTime

data class EnrollmentResponse(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val courseId: Long,
    val courseName: String,
    val enrollmentTime: LocalDateTime
)
