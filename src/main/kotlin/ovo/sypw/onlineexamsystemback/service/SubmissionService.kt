package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.GradeRequest
import ovo.sypw.onlineexamsystemback.dto.request.ProctoringEventRequest
import ovo.sypw.onlineexamsystemback.dto.request.SubmissionRequest
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse

interface SubmissionService {
    // Start exam - create initial submission record
    fun startExam(examId: Long, userId: Long): SubmissionResponse
    
    // Submit exam answers
    fun submitExam(request: SubmissionRequest, userId: Long): SubmissionResponse
    
    // Get submission by ID
    fun getSubmissionById(id: Long, userId: Long, userRole: String): SubmissionResponse
    
    // Get all submissions for an exam
    fun getExamSubmissions(examId: Long, userId: Long, userRole: String): List<SubmissionResponse>
    
    // Get user's all submissions
    fun getUserSubmissions(userId: Long): List<SubmissionResponse>
    
    // Manual grading for subjective questions
    fun gradeSubmission(id: Long, request: GradeRequest, userId: Long, userRole: String): SubmissionResponse
    
    // Record proctoring event
    fun recordProctoringEvent(request: ProctoringEventRequest, userId: Long): Boolean
}
