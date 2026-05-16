package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.ProctoringDataResponse
import ovo.sypw.onlineexamsystemback.dto.response.ProctoringEventResponse
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SubmissionService {
    // Start exam - create initial submission record
    fun startExam(examId: Long, userId: Long): SubmissionResponse

    // Submit exam answers
    fun submitExam(request: SubmissionRequest, userId: Long): SubmissionResponse

    // Get submission by ID
    fun getSubmissionById(id: Long, userId: Long, userRole: String): SubmissionResponse

    // Get all submissions for an exam
    fun getExamSubmissions(examId: Long, userId: Long, userRole: String, pageable: Pageable): Page<SubmissionResponse>

    // Get user's all submissions (paginated)
    fun getUserSubmissions(userId: Long, pageable: Pageable): Page<SubmissionResponse>

    // Get user's submission for a specific exam (returns single submission as a page)
    fun getUserSubmissionByExamId(examId: Long, userId: Long): Page<SubmissionResponse>

    // Manual grading for subjective questions
    fun gradeSubmission(id: Long, request: GradeRequest, userId: Long, userRole: String): SubmissionResponse

    // Record proctoring event
    fun recordProctoringEvent(request: ProctoringEventRequest, userId: Long): ProctoringEventResponse

    // Get proctoring data for a submission
    fun getProctoringData(submissionId: Long, userId: Long, userRole: String): ProctoringDataResponse
}
