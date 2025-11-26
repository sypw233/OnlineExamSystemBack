package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import ovo.sypw.onlineexamsystemback.dto.response.ImportResultResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.QuestionImportExportService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/questions")
@Tag(name = "题目导入导出", description = "Excel批量导入导出题目")
class QuestionImportExportController(
    private val questionImportExportService: QuestionImportExportService,
    private val userRepository: UserRepository
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
            
            ## 数据验证
            - 题型有效性检查
            - 选项完整性（单选/多选必须4个选项）
            - 答案格式验证
            - 必填字段检查
            
            ## 错误处理
            - 导入失败的行会在errors中列出
            - 成功的行会正常导入
            - 最多返回前20个错误
            
            ## 权限
            - 仅教师和管理员可导入
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun importQuestions(
        @Parameter(description = "Excel文件", required = true)
        @RequestParam("file") file: MultipartFile,
        
        @Parameter(description = "题库ID", example = "1", required = true)
        @RequestParam("bankId") bankId: Long
    ): Result<ImportResultResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        // Only teacher and admin can import
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以导入题目", 403)
        }

        // Validate file
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
                creatorId = user.id ?: 0L
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
            - 文件名格式：{题库名}_questions.xlsx
            
            ## 导出内容
            - 题型、题目内容、选项
            - 答案、解析
            - 难度、标签
            
            ## 用途
            - 备份题库
            - 分享题目
            - 在其他系统中使用
            
            ## 权限
            - 仅教师和管理员可导出
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun exportQuestions(
        @Parameter(description = "题库ID", example = "1", required = true)
        @RequestParam("bankId") bankId: Long,
        
        response: HttpServletResponse
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("未登录")

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalStateException("用户不存在")

        // Only teacher and admin can export
        if (user.role != "teacher" && user.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以导出题库")
        }

        questionImportExportService.exportQuestionsToExcel(bankId, response)
    }

    @GetMapping("/template")
    @Operation(
        summary = "下载导入模板",
        description = """
            下载题目导入Excel模板
            
            ## 功能说明
            - 下载预设格式的Excel模板
            - 包含示例数据
            - 包含各种题型示例
            
            ## 模板内容
            - 正确的列名和顺序
            - 5个示例题目（单选、多选、判断、填空、简答）
            - 格式说明
            
            ## 使用方法
            1. 下载模板
            2. 按照示例填写题目
            3. 上传导入
            
            ## 权限
            - 所有教师和管理员可下载
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun downloadTemplate(
        response: HttpServletResponse
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("未登录")

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalStateException("用户不存在")

        // Only teacher and admin can download template
        if (user.role != "teacher" && user.role != "admin") {
            throw IllegalArgumentException("只有教师和管理员可以下载模板")
        }

        questionImportExportService.downloadImportTemplate(response)
    }
}
