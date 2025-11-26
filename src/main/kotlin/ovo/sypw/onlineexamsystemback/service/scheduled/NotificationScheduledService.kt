package ovo.sypw.onlineexamsystemback.service.scheduled

import ovo.sypw.onlineexamsystemback.repository.CourseSelectionRepository
import ovo.sypw.onlineexamsystemback.repository.ExamRepository
import ovo.sypw.onlineexamsystemback.service.NotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationScheduledService(
    private val examRepository: ExamRepository,
    private val courseSelectionRepository: CourseSelectionRepository,
    private val notificationService: NotificationService
) {

    /**
     * Send exam reminders 24 hours before exam
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    fun sendExamReminder24Hours() {
        val now = LocalDateTime.now()
        val in24Hours = now.plusHours(24)
        val in25Hours = now.plusHours(25)

        // Find exams starting in 24-25 hours
        val upcomingExams = examRepository.findAll().filter { exam ->
            exam.startTime.isAfter(in24Hours) && exam.startTime.isBefore(in25Hours) && exam.status == 1
        }

        upcomingExams.forEach { exam ->
            // Get students enrolled in the course
            val courseId = exam.courseId
            val selections = courseSelectionRepository.findByCourseId(courseId)
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
    @Scheduled(cron = "0 */10 * * * *")  // Every 10 minutes
    fun sendExamReminder1Hour() {
        val now = LocalDateTime.now()
        val in60Minutes = now.plusMinutes(60)
        val in70Minutes = now.plusMinutes(70)

        // Find exams starting in 60-70 minutes
        val upcomingExams = examRepository.findAll().filter { exam ->
            exam.startTime.isAfter(in60Minutes) && exam.startTime.isBefore(in70Minutes) && exam.status == 1
        }

        upcomingExams.forEach { exam ->
            // Get students enrolled in the course
            val courseId = exam.courseId
            val selections = courseSelectionRepository.findByCourseId(courseId)
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
        // This can be implemented later if needed
        // notificationRepository.deleteOldReadNotifications(...)
    }
}
