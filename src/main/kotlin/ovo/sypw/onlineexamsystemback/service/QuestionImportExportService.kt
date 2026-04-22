package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.response.ImportResultResponse
import org.springframework.web.multipart.MultipartFile

interface QuestionImportExportService {
    /**
     * Import questions from Excel file
     * @param file Excel file
     * @param bankId Question bank ID to import into
     * @param creatorId User ID of the creator
     * @return Import result with success/failure counts
     */
    fun importQuestionsFromExcel(
        file: MultipartFile,
        bankId: Long,
        creatorId: Long
    ): ImportResultResponse

    /**
     * Export questions to Excel file
     * @param bankId Question bank ID to export from
     * @param userId Exporter user ID
     * @return Excel file bytes and filename
     */
    fun exportQuestionsToExcel(bankId: Long, userId: Long): Pair<ByteArray, String>

    /**
     * Generate import template
     * @param userId Downloader user ID
     * @return Excel file bytes and filename
     */
    fun downloadImportTemplate(userId: Long): Pair<ByteArray, String>
}
