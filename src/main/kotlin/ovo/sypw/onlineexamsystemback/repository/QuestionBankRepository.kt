package ovo.sypw.onlineexamsystemback.repository

import ovo.sypw.onlineexamsystemback.entity.QuestionBank
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestionBankRepository : JpaRepository<QuestionBank, Long> {
    // Find banks by creator
    fun findByCreatorId(creatorId: Long): List<QuestionBank>
    
    // Check ownership
    fun existsByIdAndCreatorId(id: Long, creatorId: Long): Boolean
    
    // Find by name (for search)
    fun findByNameContaining(name: String): List<QuestionBank>
}
