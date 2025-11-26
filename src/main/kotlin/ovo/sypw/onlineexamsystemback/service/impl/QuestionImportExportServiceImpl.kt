package ovo.sypw.onlineexamsystemback.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import ovo.sypw.onlineexamsystemback.dto.response.ImportErrorDetail
import ovo.sypw.onlineexamsystemback.dto.response.ImportResultResponse
import ovo.sypw.onlineexamsystemback.entity.Question
import ovo.sypw.onlineexamsystemback.entity.QuestionBankQuestion
import ovo.sypw.onlineexamsystemback.repository.QuestionBankQuestionRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionBankRepository
import ovo.sypw.onlineexamsystemback.repository.QuestionRepository
import ovo.sypw.onlineexamsystemback.service.QuestionImportExportService
import java.util.*

@Service
@Transactional
class QuestionImportExportServiceImpl(
    private val questionRepository: QuestionRepository,
    private val questionBankRepository: QuestionBankRepository,
    private val questionBankQuestionRepository: QuestionBankQuestionRepository,
    private val objectMapper: ObjectMapper
) : QuestionImportExportService {

    companion object {
        private val VALID_TYPES = setOf("single", "multiple", "true_false", "fill_blank", "short_answer")
        private val VALID_DIFFICULTIES = setOf("easy", "medium", "hard")
        
        // Excel column indexes
        private const val COL_TYPE = 0
        private const val COL_CONTENT = 1
        private const val COL_OPTION_A = 2
        private const val COL_OPTION_B = 3
        private const val COL_OPTION_C = 4
        private const val COL_OPTION_D = 5
        private const val COL_ANSWER = 6
        private const val COL_EXPLANATION = 7
        private const val COL_DIFFICULTY = 8
        private const val COL_TAGS = 9
    }

    override fun importQuestionsFromExcel(
        file: MultipartFile,
        bankId: Long,
        creatorId: Long
    ): ImportResultResponse {
        // Validate bank exists
        questionBankRepository.findById(bankId).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        val workbook = XSSFWorkbook(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        
        val errors = mutableListOf<ImportErrorDetail>()
        val successQuestions = mutableListOf<Question>()
        var totalRows = 0

        // Start from row 1 (skip header row 0)
        for (rowIndex in 1 until sheet.physicalNumberOfRows) {
            totalRows++
            val row = sheet.getRow(rowIndex) ?: continue
            
            try {
                val question = parseQuestionFromRow(row, rowIndex + 1, creatorId)
                successQuestions.add(question)
            } catch (e: Exception) {
                errors.add(ImportErrorDetail(rowIndex + 1, e.message ?: "未知错误"))
            }
        }

        // Batch save successful questions
        val savedQuestions = questionRepository.saveAll(successQuestions)
        
        // Link questions to bank
        val bankQuestions = savedQuestions.map { question ->
            QuestionBankQuestion(
                bankId = bankId,
                questionId = question.id ?: 0L
            )
        }
        questionBankQuestionRepository.saveAll(bankQuestions)

        workbook.close()

        return ImportResultResponse(
            taskId = UUID.randomUUID().toString(),
            totalRows = totalRows,
            successCount = successQuestions.size,
            failedCount = errors.size,
            errors = errors.take(20) // Limit to first 20 errors
        )
    }

    private fun parseQuestionFromRow(row: Row, rowNumber: Int, creatorId: Long): Question {
        val type = getCellValue(row, COL_TYPE).trim().lowercase()
        val content = getCellValue(row, COL_CONTENT).trim()
        val optionA = getCellValue(row, COL_OPTION_A).trim()
        val optionB = getCellValue(row, COL_OPTION_B).trim()
        val optionC = getCellValue(row, COL_OPTION_C).trim()
        val optionD = getCellValue(row, COL_OPTION_D).trim()
        val answer = getCellValue(row, COL_ANSWER).trim()
        val analysis = getCellValue(row, COL_EXPLANATION).trim()
        val difficulty = getCellValue(row, COL_DIFFICULTY).trim().lowercase()
        val category = getCellValue(row, COL_TAGS).trim()

        // Validate type
        if (type !in VALID_TYPES) {
            throw IllegalArgumentException("题型无效: $type，有效值: ${VALID_TYPES.joinToString()}")
        }

        // Validate content
        if (content.isBlank()) {
            throw IllegalArgumentException("题目内容不能为空")
        }

        // Validate options for objective questions
        if (type in setOf("single", "multiple")) {
            if (optionA.isBlank() || optionB.isBlank() || optionC.isBlank() || optionD.isBlank()) {
                throw IllegalArgumentException("单选题和多选题必须提供4个选项")
            }
        }

        // Validate answer
        if (answer.isBlank()) {
            throw IllegalArgumentException("答案不能为空")
        }

        // Validate answer format for multiple choice
        if (type == "multiple") {
            val options = answer.split(",").map { it.trim().uppercase() }
            if (options.any { it !in setOf("A", "B", "C", "D") }) {
                throw IllegalArgumentException("多选题答案格式错误，应为: A,C (用逗号分隔)")
            }
        }

        // Validate difficulty
        if (difficulty.isNotBlank() && difficulty !in VALID_DIFFICULTIES) {
            throw IllegalArgumentException("难度无效: $difficulty，有效值: ${VALID_DIFFICULTIES.joinToString()}")
        }

        // Build options JSON
        val options = if (type in setOf("single", "multiple")) {
            objectMapper.writeValueAsString(mapOf(
                "A" to optionA,
                "B" to optionB,
                "C" to optionC,
                "D" to optionD
            ))
        } else if (type == "true_false") {
            objectMapper.writeValueAsString(mapOf(
                "A" to "正确",
                "B" to "错误"
            ))
        } else {
            null
        }

        return Question(
            type = type,
            content = content,
            options = options,
            answer = answer.uppercase(),
            analysis = analysis.ifBlank { null },
            difficulty = difficulty.ifBlank { "medium" },
            category = category.ifBlank { null },
            creatorId = creatorId
        )
    }

    private fun getCellValue(row: Row, columnIndex: Int): String {
        val cell = row.getCell(columnIndex) ?: return ""
        
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }

    override fun exportQuestionsToExcel(bankId: Long, response: HttpServletResponse) {
        // Validate bank exists
        val bank = questionBankRepository.findById(bankId).orElseThrow {
            throw IllegalArgumentException("题库不存在")
        }

        // Get all questions in the bank
        val bankQuestions = questionBankQuestionRepository.findByBankId(bankId)
        val questionIds = bankQuestions.map { it.questionId }
        val questions = questionRepository.findAllById(questionIds)

        // Create workbook
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("题目列表")

        // Create header style
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply {
                bold = true
            }
            setFont(font)
        }

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf("题型", "题目内容", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析", "难度", "分类")
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = headerStyle
            }
        }

        // Fill data rows
        questions.forEachIndexed { index, question ->
            val row = sheet.createRow(index + 1)
            
            row.createCell(COL_TYPE).setCellValue(question.type)
            row.createCell(COL_CONTENT).setCellValue(question.content)

            // Parse options
            if (question.options != null) {
                val optionsMap = objectMapper.readValue(question.options!!, Map::class.java) as Map<String, String>
                row.createCell(COL_OPTION_A).setCellValue(optionsMap["A"] ?: "")
                row.createCell(COL_OPTION_B).setCellValue(optionsMap["B"] ?: "")
                row.createCell(COL_OPTION_C).setCellValue(optionsMap["C"] ?: "")
                row.createCell(COL_OPTION_D).setCellValue(optionsMap["D"] ?: "")
            }

            row.createCell(COL_ANSWER).setCellValue(question.answer ?: "")
            row.createCell(COL_EXPLANATION).setCellValue(question.analysis ?: "")
            row.createCell(COL_DIFFICULTY).setCellValue(question.difficulty)
            row.createCell(COL_TAGS).setCellValue(question.category ?: "")
        }

        // Auto-size columns
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }

        // Set response headers
        response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        response.setHeader("Content-Disposition", "attachment; filename=\"${bank.name}_questions.xlsx\"")

        // Write to response
        workbook.write(response.outputStream)
        workbook.close()
    }

    override fun downloadImportTemplate(response: HttpServletResponse) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("题目导入模板")

        // Create header style
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
        }

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf("题型", "题目内容", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析", "难度", "分类")
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = headerStyle
            }
        }

        // Add example rows
        val examples = listOf(
            arrayOf("single", "Java是什么类型的语言？", "编译型语言", "解释型语言", "混合型语言", "标记语言", "C", "Java是编译+解释的混合型语言", "easy", "Java基础"),
            arrayOf("multiple", "以下哪些是Java关键字？", "class", "Class", "public", "Public", "A,C", "注意大小写", "medium", "关键字"),
            arrayOf("true_false", "Java是面向对象的语言", "", "", "", "", "A", "A表示正确，B表示错误", "easy", "Java基础"),
            arrayOf("fill_blank", "Java的父类是____", "", "", "", "", "Object", "所有类的父类都是Object", "medium", "继承"),
            arrayOf("short_answer", "简述Java多态的概念", "", "", "", "", "多态是指同一个接口可以有多种实现...", "需教师手动评分", "hard", "多态")
        )

        examples.forEachIndexed { index, example ->
            val row = sheet.createRow(index + 1)
            example.forEachIndexed { cellIndex, value ->
                row.createCell(cellIndex).setCellValue(value)
            }
        }

        // Auto-size columns
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }

        // Set response headers
        response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        response.setHeader("Content-Disposition", "attachment; filename=\"question_import_template.xlsx\"")

        // Write to response
        workbook.write(response.outputStream)
        workbook.close()
    }
}
