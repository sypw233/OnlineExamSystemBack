package ovo.sypw.onlineexamsystemback.service.scheduled

import ovo.sypw.onlineexamsystemback.repository.ExamRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ExamScheduledService(
    private val examRepository: ExamRepository
) {

    private val logger = LoggerFactory.getLogger(ExamScheduledService::class.java)

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
            logger.info("[Scheduled] Auto-ended {} expired exams", expiredExams.size)
        }
    }
}
