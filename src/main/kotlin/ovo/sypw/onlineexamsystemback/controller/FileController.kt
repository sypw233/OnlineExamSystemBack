package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ovo.sypw.onlineexamsystemback.dto.response.FileUploadResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.FileService
import ovo.sypw.onlineexamsystemback.util.Result

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Upload, delete and access files")
class FileController(
    private val fileService: FileService
) {

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload image", security = [SecurityRequirement(name = "Bearer Authentication")])
    fun uploadImage(
        @CurrentUser user: User,
        @Parameter(
            description = "Image file",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart("file") file: MultipartFile,
        @Parameter(
            description = "File category",
            example = "questions",
            schema = Schema(allowableValues = ["questions", "avatars", "temp"])
        )
        @RequestParam(defaultValue = "temp") category: String
    ): Result<FileUploadResponse> {
        if (!canUploadImage(user, category)) {
            return Result.error("No permission to upload this image category", 403)
        }
        return try {
            val response = fileService.uploadImage(file, category, requireUserId(user))
            Result.success(response, "Image uploaded")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "Upload failed", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping("/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload document", security = [SecurityRequirement(name = "Bearer Authentication")])
    fun uploadDocument(
        @CurrentUser user: User,
        @Parameter(
            description = "Document file",
            required = true,
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart("file") file: MultipartFile,
        @Parameter(
            description = "File category",
            example = "attachments",
            schema = Schema(allowableValues = ["attachments", "materials", "temp"])
        )
        @RequestParam(defaultValue = "temp") category: String
    ): Result<FileUploadResponse> {
        return try {
            val response = fileService.uploadDocument(file, category, requireUserId(user))
            Result.success(response, "Document uploaded")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "Upload failed", 400)
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{*fileKey}")
    @Operation(summary = "Delete file", security = [SecurityRequirement(name = "Bearer Authentication")])
    fun deleteFile(
        @CurrentUser user: User,
        @Parameter(
            description = "File key",
            example = "images/questions/20241126/abc123.jpg"
        )
        @PathVariable fileKey: String
    ): Result<String> {
        return try {
            fileService.deleteFile(fileKey, requireUserId(user), user.role)
            Result.success("Deleted")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "Delete failed", 400)
        }
    }

    @GetMapping("/url/{*fileKey}")
    @Operation(summary = "Get file url", security = [SecurityRequirement(name = "Bearer Authentication")])
    fun getFileUrl(
        @CurrentUser user: User,
        @Parameter(
            description = "File key",
            example = "images/questions/20241126/abc123.jpg"
        )
        @PathVariable fileKey: String
    ): Result<Map<String, String>> {
        return try {
            val url = fileService.getFileUrl(fileKey)
            Result.success(mapOf("url" to url))
        } catch (e: Exception) {
            Result.error(e.message ?: "Get URL failed", 400)
        }
    }

    private fun canUploadImage(user: User, category: String): Boolean {
        val normalizedRole = user.role.uppercase()
        return when (category.lowercase()) {
            "avatars" -> true
            "questions", "temp" -> normalizedRole == "TEACHER" || normalizedRole == "ADMIN"
            else -> false
        }
    }

    private fun requireUserId(user: User): Long =
        user.id ?: throw IllegalStateException("User has no persisted id")
}
