package ovo.sypw.onlineexamsystemback.dto.response

import java.time.LocalDateTime

data class CourseResponse(
    val id: Long,
    val courseName: String,
    val description: String?,
    val teacherId: Long,
    val teacherName: String,
    val status: Int,
    val enrollmentCount: Long,
    val createTime: LocalDateTime
)
