package ovo.sypw.onlineexamsystemback.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "exams")
data class Exam(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "course_id", nullable = false)
    val courseId: Long,

    @Column(name = "creator_id", nullable = false)
    val creatorId: Long,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalDateTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalDateTime,

    @Column
    var duration: Int? = null, // Duration in minutes

    @Column(name = "total_score", nullable = false)
    var totalScore: Int = 100,

    @Column(nullable = false)
    var status: Int = 0, // 0-Draft, 1-Published, 2-Ended

    @Column(name = "needs_grading", nullable = false)
    var needsGrading: Boolean = false,

    // 监考控制字段
    @Column(name = "allowed_platforms", length = 50)
    var allowedPlatforms: String? = null, // "desktop", "mobile", "both"

    @Column(name = "strict_mode", nullable = false)
    var strictMode: Boolean = false, // 是否开启严格监考

    @Column(name = "max_switch_count")
    var maxSwitchCount: Int? = null, // 最大允许切出次数（null表示无限制）

    @Column(name = "fullscreen_required", nullable = false)
    var fullscreenRequired: Boolean = false, // 是否要求全屏（仅桌面端）

    @Column(name = "create_time", nullable = false, updatable = false)
    val createTime: LocalDateTime = LocalDateTime.now()
)
