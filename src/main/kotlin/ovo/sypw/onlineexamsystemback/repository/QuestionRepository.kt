package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    // Find questions by creator
    fun findByCreatorId(creatorId: Long): List<Question>
    
    // Filter by question type
    fun findByType(type: String): List<Question>
    
    // Filter by difficulty
    fun findByDifficulty(difficulty: String): List<Question>
    
    // Filter by category
    fun findByCategory(category: String): List<Question>
    
    // Combination filters
    fun findByCreatorIdAndType(creatorId: Long, type: String): List<Question>
    fun findByCreatorIdAndDifficulty(creatorId: Long, difficulty: String): List<Question>
    
    // Check ownership
    fun existsByIdAndCreatorId(id: Long, creatorId: Long): Boolean
}
