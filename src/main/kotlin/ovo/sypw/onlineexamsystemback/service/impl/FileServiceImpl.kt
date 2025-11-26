package ovo.sypw.onlineexamsystemback.service.impl

import com.baidubce.services.bos.BosClient
import com.baidubce.services.bos.model.ObjectMetadata
import ovo.sypw.onlineexamsystemback.config.BosProperties
import ovo.sypw.onlineexamsystemback.dto.response.FileUploadResponse
import ovo.sypw.onlineexamsystemback.service.FileService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FileServiceImpl(
    private val bosClient: BosClient,
    private val bosProperties: BosProperties
) : FileService {

    companion object {
        // File size limits
        private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024L  // 5MB
        private const val MAX_DOCUMENT_SIZE = 20 * 1024 * 1024L  // 20MB
        
        // Allowed content types
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/webp", "image/bmp", "image/svg+xml"
        )
        
        private val ALLOWED_DOCUMENT_TYPES = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "application/zip",
            "application/x-rar-compressed"
        )
    }

    override fun uploadImage(file: MultipartFile, category: String, userId: Long): FileUploadResponse {
        // Validate file
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, "图片")
        
        // Generate file key
        val fileKey = generateFileKey("images", category, file.originalFilename ?: "image")
        
        // Upload to BOS
        uploadToBos(file, fileKey)
        
        return FileUploadResponse(
            fileKey = fileKey,
            fileUrl = getFileUrl(fileKey),
            fileName = file.originalFilename ?: "unknown",
            fileSize = file.size
        )
    }

    override fun uploadDocument(file: MultipartFile, category: String, userId: Long): FileUploadResponse {
        // Validate file
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE, "文档")
        
        // Generate file key
        val fileKey = generateFileKey("documents", category, file.originalFilename ?: "document")
        
        // Upload to BOS
        uploadToBos(file, fileKey)
        
        return FileUploadResponse(
            fileKey = fileKey,
            fileUrl = getFileUrl(fileKey),
            fileName = file.originalFilename ?: "unknown",
            fileSize = file.size
        )
    }

    override fun deleteFile(fileKey: String, userId: Long, userRole: String) {
        try {
            bosClient.deleteObject(bosProperties.bucketName, fileKey)
        } catch (e: Exception) {
            throw IllegalArgumentException("删除文件失败: ${e.message}")
        }
    }

    override fun getFileUrl(fileKey: String): String {
        // Public read URL format
        return "https://${bosProperties.bucketName}.${bosProperties.endpoint}/${fileKey}"
    }

    /**
     * Validate file type and size
     */
    private fun validateFile(
        file: MultipartFile,
        allowedTypes: Set<String>,
        maxSize: Long,
        fileTypeLabel: String
    ) {
        if (file.isEmpty) {
            throw IllegalArgumentException("文件不能为空")
        }
        
        val contentType = file.contentType
        if (contentType == null || !allowedTypes.contains(contentType.lowercase())) {
            throw IllegalArgumentException(
                "不支持的${fileTypeLabel}格式: $contentType。支持的格式: ${allowedTypes.joinToString()}"
            )
        }
        
        if (file.size > maxSize) {
            val maxSizeMB = maxSize / (1024 * 1024)
            throw IllegalArgumentException("${fileTypeLabel}大小不能超过 ${maxSizeMB}MB")
        }
    }

    /**
     * Generate unique file key
     * Format: {type}/{category}/{date}/{uuid}.{ext}
     * Example: images/questions/20241126/abc-123-def.jpg
     */
    private fun generateFileKey(type: String, category: String, originalFilename: String): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val uuid = UUID.randomUUID().toString()
        val extension = originalFilename.substringAfterLast(".", "")
        
        return "$type/$category/$date/$uuid${if (extension.isNotEmpty()) ".$extension" else ""}"
    }

    /**
     * Upload file to BOS
     */
    private fun uploadToBos(file: MultipartFile, fileKey: String) {
        try {
            val metadata = ObjectMetadata()
            metadata.contentType = file.contentType
            metadata.contentLength = file.size
            
            bosClient.putObject(
                bosProperties.bucketName,
                fileKey,
                file.inputStream,
                metadata
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("文件上传失败: ${e.message}")
        }
    }
}
