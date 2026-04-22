package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.Question
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    // Find questions by creator
    fun findByCreatorId(creatorId: Long): List<Question>
    fun findByCreatorId(creatorId: Long, pageable: Pageable): Page<Question>

    // Filter by question type
    fun findByType(type: String): List<Question>
    fun findByType(type: String, pageable: Pageable): Page<Question>

    // Filter by difficulty
    fun findByDifficulty(difficulty: String): List<Question>
    fun findByDifficulty(difficulty: String, pageable: Pageable): Page<Question>

    // Filter by category
    fun findByCategory(category: String): List<Question>
    fun findByCategory(category: String, pageable: Pageable): Page<Question>

    // Combination filters
    fun findByCreatorIdAndType(creatorId: Long, type: String): List<Question>
    fun findByCreatorIdAndDifficulty(creatorId: Long, difficulty: String): List<Question>

    // Check ownership
    fun existsByIdAndCreatorId(id: Long, creatorId: Long): Boolean

    // Unified search with optional filters
    @Query("SELECT q FROM Question q WHERE " +
            "(:creatorId IS NULL OR q.creatorId = :creatorId) AND " +
            "(:type IS NULL OR q.type = :type) AND " +
            "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
            "(:category IS NULL OR q.category = :category)")
    fun searchQuestions(
        @org.springframework.data.repository.query.Param("creatorId") creatorId: Long?,
        @org.springframework.data.repository.query.Param("type") type: String?,
        @org.springframework.data.repository.query.Param("difficulty") difficulty: String?,
        @org.springframework.data.repository.query.Param("category") category: String?,
        pageable: Pageable
    ): Page<Question>
}
