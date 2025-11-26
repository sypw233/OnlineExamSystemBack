package ovo.sypw.onlineexamsystemback.enums

enum class NotificationType(val description: String) {
    EXAM_PUBLISHED("考试发布"),
    EXAM_REMINDER("考试提醒"),
    GRADE_RELEASED("成绩发布"),
    COURSE_UPDATE("课程更新"),
    SYSTEM_ANNOUNCEMENT("系统公告")
}
