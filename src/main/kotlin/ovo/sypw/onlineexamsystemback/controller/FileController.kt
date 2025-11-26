package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import ovo.sypw.onlineexamsystemback.dto.response.FileUploadResponse
import ovo.sypw.onlineexamsystemback.repository.UserRepository
import ovo.sypw.onlineexamsystemback.service.FileService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传、删除和访问")
class FileController(
    private val fileService: FileService,
    private val userRepository: UserRepository
) {

    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "上传图片",
        description = """
            上传图片文件到BOS存储
            
            ## 支持的图片格式
            - JPEG/JPG
            - PNG
            - GIF
            - WEBP
            - BMP
            - SVG
            
            ## 文件大小限制
            - 最大 5MB
            
            ## 分类说明
            - `questions`: 题目配图
            - `avatars`: 用户头像
            - `temp`: 临时文件
            
            ## 权限
            - 仅教师和管理员可以上传
            
            ## 返回
            - 文件URL可直接用于访问（公共读）
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun uploadImage(
        @Parameter(
            description = "图片文件",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart("file") file: MultipartFile,
        
        @Parameter(
            description = "文件分类",
            example = "questions",
            schema = Schema(allowableValues = ["questions", "avatars", "temp"])
        )
        @RequestParam(defaultValue = "temp") category: String
    ): Result<FileUploadResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        // Only teacher and admin can upload
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以上传文件", 403)
        }

        return try {
            val response = fileService.uploadImage(file, category, user.id ?: 0L)
            Result.success(response, "图片上传成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "上传失败", 400)
        }
    }

    @PostMapping("/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "上传文档",
        description = """
            上传文档文件到BOS存储
            
            ## 支持的文档格式
            - PDF
            - Word (DOC, DOCX)
            - Excel (XLS, XLSX)
            - PowerPoint (PPT, PPTX)
            - 文本文件 (TXT, CSV)
            - 压缩文件 (ZIP, RAR)
            
            ## 文件大小限制
            - 最大 20MB
            
            ## 分类说明
            - `attachments`: 题目附件
            - `materials`: 参考资料
            - `temp`: 临时文件
            
            ## 权限
            - 仅教师和管理员可以上传
            
            ## 返回
            - 文件URL可直接用于下载（公共读）
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun uploadDocument(
        @Parameter(
            description = "文档文件",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart("file") file: MultipartFile,
        
        @Parameter(
            description = "文件分类",
            example = "attachments",
            schema = Schema(allowableValues = ["attachments", "materials", "temp"])
        )
        @RequestParam(defaultValue = "temp") category: String
    ): Result<FileUploadResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        // Only teacher and admin can upload
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以上传文件", 403)
        }

        return try {
            val response = fileService.uploadDocument(file, category, user.id ?: 0L)
            Result.success(response, "文档上传成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "上传失败", 400)
        }
    }

    @DeleteMapping("/{*fileKey}")
    @Operation(
        summary = "删除文件",
        description = """
            从BOS存储中删除文件
            
            ## 权限
            - 管理员可以删除任何文件
            - 教师只能删除自己上传的文件
            
            ## 注意
            - 文件删除后无法恢复
            - fileKey 是完整的文件路径，例如: images/questions/20241126/abc123.jpg
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteFile(
        @Parameter(
            description = "文件Key（完整路径）",
            example = "images/questions/20241126/abc123.jpg"
        )
        @PathVariable fileKey: String
    ): Result<String> {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: return Result.error("未登录", 401)

        val username = authentication.name
        val user = userRepository.findByUsername(username)
            ?: return Result.error("用户不存在", 404)

        // Only teacher and admin can delete
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以删除文件", 403)
        }

        return try {
            fileService.deleteFile(fileKey, user.id ?: 0L, user.role)
            Result.success("删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @GetMapping("/url/{*fileKey}")
    @Operation(
        summary = "获取文件URL",
        description = """
            获取文件的公共访问URL
            
            ## 说明
            - 返回的URL可以直接在浏览器中访问
            - 所有文件都是公共读权限
            - 无需认证即可访问文件
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getFileUrl(
        @Parameter(
            description = "文件Key（完整路径）",
            example = "images/questions/20241126/abc123.jpg"
        )
        @PathVariable fileKey: String
    ): Result<Map<String, String>> {
        return try {
            val url = fileService.getFileUrl(fileKey)
            Result.success(mapOf("url" to url))
        } catch (e: Exception) {
            Result.error(e.message ?: "获取URL失败", 400)
        }
    }
}
