package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import ovo.sypw.onlineexamsystemback.dto.response.ImportResultResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.QuestionImportExportService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/question-import-export")
@Tag(name = "题目导入导出", description = "Excel批量导入导出题目")
class QuestionImportExportController(
    private val questionImportExportService: QuestionImportExportService
) {

    @PostMapping("/import")
    @Operation(
        summary = "导入题目",
        description = """
            从Excel文件批量导入题目到题库
            
            ## 功能说明
            - 支持Excel格式文件（.xlsx）
            - 自动验证题目数据
            - 返回导入结果和错误详情
            
            ## Excel格式
            必须包含以下列（按顺序）：
            1. 题型（single/multiple/true_false/fill_blank/short_answer）
            2. 题目内容
            3. 选项A
            4. 选项B
            5. 选项C
            6. 选项D
            7. 正确答案
            8. 解析
            9. 难度（easy/medium/hard）
            10. 标签（逗号分隔）
            
            ## 权限
            - 仅教师和管理员可导入
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun importQuestions(
        @CurrentUser user: User,
        @Parameter(description = "Excel文件", required = true)
        @RequestParam("file") file: MultipartFile,

        @Parameter(description = "题库ID", example = "1", required = true)
        @RequestParam("bankId") bankId: Long
    ): Result<ImportResultResponse> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以导入题目", 403)
        }

        if (file.isEmpty) {
            return Result.error("文件不能为空", 400)
        }

        if (!file.originalFilename!!.endsWith(".xlsx")) {
            return Result.error("仅支持.xlsx格式文件", 400)
        }

        return try {
            val result = questionImportExportService.importQuestionsFromExcel(
                file = file,
                bankId = bankId,
                creatorId = user.safeId
            )
            Result.success(result, "导入完成")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "导入失败", 400)
        } catch (e: Exception) {
            Result.error("文件解析失败: ${e.message}", 500)
        }
    }

    @GetMapping("/export")
    @Operation(
        summary = "导出题库",
        description = """
            导出题库中的所有题目为Excel文件
            
            ## 功能说明
            - 导出指定题库的全部题目
            - Excel格式，包含所有题目信息
            - 浏览器直接下载
            
            ## 权限
            - 仅教师和管理员可导出
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun exportQuestions(
        @CurrentUser user: User,
        @Parameter(description = "题库ID", example = "1", required = true)
        @RequestParam("bankId") bankId: Long
    ): ResponseEntity<ByteArray> {
        if (user.role != "teacher" && user.role != "admin") {
            return ResponseEntity.status(403)
                .body("只有教师和管理员可以导出题库".toByteArray())
        }

        return try {
            val (bytes, filename) = questionImportExportService.exportQuestionsToExcel(bankId, user.safeId)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                setContentDispositionFormData("attachment", filename)
            }
            ResponseEntity.ok()
                .headers(headers)
                .body(bytes)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(400).body((e.message ?: "导出失败").toByteArray())
        } catch (e: Exception) {
            ResponseEntity.status(500).body("导出失败: ${e.message}".toByteArray())
        }
    }

    @GetMapping("/template")
    @Operation(
        summary = "下载导入模板",
        description = """
            下载题目导入Excel模板
            
            ## 功能说明
            - 下载预设格式的Excel模板
            - 包含示例数据
            - 浏览器直接下载
            
            ## 权限
            - 所有教师和管理员可下载
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun downloadTemplate(
        @CurrentUser user: User
    ): ResponseEntity<ByteArray> {
        if (user.role != "teacher" && user.role != "admin") {
            return ResponseEntity.status(403)
                .body("只有教师和管理员可以下载模板".toByteArray())
        }

        return try {
            val (bytes, filename) = questionImportExportService.downloadImportTemplate(user.safeId)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                setContentDispositionFormData("attachment", filename)
            }
            ResponseEntity.ok()
                .headers(headers)
                .body(bytes)
        } catch (e: Exception) {
            ResponseEntity.status(500).body("模板生成失败: ${e.message}".toByteArray())
        }
    }
}
