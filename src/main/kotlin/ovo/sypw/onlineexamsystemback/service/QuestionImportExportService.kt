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
     * Export questions to Excel file and upload to BOS
     * @param bankId Question bank ID to export from
     * @param userId Exporter user ID
     * @return Download URL of the exported Excel file
     */
    fun exportQuestionsToExcel(bankId: Long, userId: Long): String

    /**
     * Generate import template and upload to BOS
     * @param userId Downloader user ID
     * @return Download URL of the template file
     */
    fun downloadImportTemplate(userId: Long): String
}
