package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.response.ImportResultResponse
import org.springframework.web.multipart.MultipartFile
import jakarta.servlet.http.HttpServletResponse

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
     * @param response HTTP response to write Excel file to
     */
    fun exportQuestionsToExcel(bankId: Long, response: HttpServletResponse)
    
    /**
     * Generate and download import template
     * @param response HTTP response to write template to
     */
    fun downloadImportTemplate(response: HttpServletResponse)
}
