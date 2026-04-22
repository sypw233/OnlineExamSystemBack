package ovo.sypw.onlineexamsystemback.service.scheduled

import ovo.sypw.onlineexamsystemback.repository.CourseSelectionRepository
import ovo.sypw.onlineexamsystemback.repository.ExamRepository
import ovo.sypw.onlineexamsystemback.repository.NotificationRepository
import ovo.sypw.onlineexamsystemback.service.NotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationScheduledService(
    private val examRepository: ExamRepository,
    private val courseSelectionRepository: CourseSelectionRepository,
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository
) {

    /**
     * Send exam reminders 24 hours before exam
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    fun sendExamReminder24Hours() {
        val now = LocalDateTime.now()
        val exams = examRepository.findByStatusAndStartTimeBetween(1, now.plusHours(24), now.plusHours(25))

        exams.forEach { exam ->
            val selections = courseSelectionRepository.findByCourseId(exam.courseId)
            val studentIds = selections.map { it.studentId }
            if (studentIds.isNotEmpty()) {
                notificationService.sendExamReminder(
                    examId = exam.id ?: 0L,
                    examTitle = exam.title,
                    studentIds = studentIds,
                    hoursBeforeExam = 24
                )
            }
        }
    }

    /**
     * Send exam reminders 1 hour before exam
     * Runs every 10 minutes
     */
    @Scheduled(cron = "0 */10 * * * *")
    fun sendExamReminder1Hour() {
        val now = LocalDateTime.now()
        val exams = examRepository.findByStatusAndStartTimeBetween(1, now.plusMinutes(60), now.plusMinutes(70))

        exams.forEach { exam ->
            val selections = courseSelectionRepository.findByCourseId(exam.courseId)
            val studentIds = selections.map { it.studentId }
            if (studentIds.isNotEmpty()) {
                notificationService.sendExamReminder(
                    examId = exam.id ?: 0L,
                    examTitle = exam.title,
                    studentIds = studentIds,
                    hoursBeforeExam = 1
                )
            }
        }
    }

    /**
     * Clean up old read notifications (older than 30 days)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun cleanupOldNotifications() {
        val beforeDate = LocalDateTime.now().minusDays(30)
        val deletedCount = notificationRepository.deleteOldReadNotifications(beforeDate)
        println("[Scheduled] Cleaned up $deletedCount old read notifications")
    }
}
