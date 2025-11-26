package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.response.*

interface StatisticsService {
    /**
     * Get exam statistics
     * @param examId Exam ID
     * @return Exam statistics including scores, pass rate, distribution
     */
    fun getExamStatistics(examId: Long): ExamStatisticsResponse
    
    /**
     * Get course statistics
     * @param courseId Course ID
     * @return Course statistics including student count, exam count, average score
     */
    fun getCourseStatistics(courseId: Long): CourseStatisticsResponse
    
    /**
     * Get question statistics
     * @param questionId Question ID
     * @return Question statistics including usage, accuracy, option distribution
     */
    fun getQuestionStatistics(questionId: Long): QuestionStatisticsResponse
    
    /**
     * Get student statistics
     * @param studentId Student ID
     * @return Student performance history and statistics
     */
    fun getStudentStatistics(studentId: Long): StudentStatisticsResponse
    
    /**
     * Get system overview (admin only)
     * @return System-wide statistics
     */
    fun getSystemOverview(): SystemOverviewResponse
}
