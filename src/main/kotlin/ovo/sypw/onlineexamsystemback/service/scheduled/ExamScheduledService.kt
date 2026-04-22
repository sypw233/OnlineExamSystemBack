package ovo.sypw.onlineexamsystemback.service.scheduled

import ovo.sypw.onlineexamsystemback.repository.ExamRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ExamScheduledService(
    private val examRepository: ExamRepository
) {

    /**
     * Auto-end published exams that have passed their end time
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    fun autoEndExams() {
        val now = LocalDateTime.now()
        val expiredExams = examRepository.findByStatusAndEndTimeBefore(1, now)

        expiredExams.forEach { exam ->
            exam.status = 2
            examRepository.save(exam)
        }

        if (expiredExams.isNotEmpty()) {
            println("[Scheduled] Auto-ended ${expiredExams.size} expired exams")
        }
    }
}
