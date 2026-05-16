package ovo.sypw.onlineexamsystemback.enums

enum class NotificationType(val description: String) {
    EXAM_PUBLISHED("考试发布"),
    EXAM_REMINDER("考试提醒"),
    EXAM_CHANGED("考试变更"),
    GRADE_RELEASED("成绩发布"),
    COURSE_UPDATE("课程更新"),
    COURSE_ENROLLED("课程选课"),
    SYSTEM_ANNOUNCEMENT("系统公告"),
    HOMEWORK_REMINDER("作业提醒")
}
