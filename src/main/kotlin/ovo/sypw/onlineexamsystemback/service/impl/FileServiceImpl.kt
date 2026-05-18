package ovo.sypw.onlineexamsystemback.service.impl

import com.baidubce.services.bos.BosClient
import com.baidubce.services.bos.model.ObjectMetadata
import ovo.sypw.onlineexamsystemback.config.BosProperties
import ovo.sypw.onlineexamsystemback.dto.response.FileUploadResponse
import ovo.sypw.onlineexamsystemback.service.FileService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FileServiceImpl(
    private val bosClient: BosClient,
    private val bosProperties: BosProperties,
    @Value("\${file.upload.path:./uploads/}") private val uploadPath: String
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
        
        val fileUrl = storeFile(file, fileKey, preferLocal = category.equals("avatars", ignoreCase = true))
        
        return FileUploadResponse(
            fileKey = fileKey,
            fileUrl = fileUrl,
            fileName = file.originalFilename ?: "unknown",
            fileSize = file.size
        )
    }

    override fun uploadDocument(file: MultipartFile, category: String, userId: Long): FileUploadResponse {
        // Validate file
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE, "文档")
        
        // Generate file key
        val fileKey = generateFileKey("documents", category, file.originalFilename ?: "document")
        
        val fileUrl = storeFile(file, fileKey)
        
        return FileUploadResponse(
            fileKey = fileKey,
            fileUrl = fileUrl,
            fileName = file.originalFilename ?: "unknown",
            fileSize = file.size
        )
    }

    override fun deleteFile(fileKey: String, userId: Long, userRole: String) {
        if (userRole != "admin" && userRole != "teacher") {
            throw IllegalArgumentException("无权删除文件")
        }
        val localDeleted = deleteLocalFile(fileKey)
        try {
            if (!localDeleted) {
                bosClient.deleteObject(bosProperties.bucketName, fileKey)
            } else {
                runCatching { bosClient.deleteObject(bosProperties.bucketName, fileKey) }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("删除文件失败: ${e.message}")
        }
    }

    override fun getFileUrl(fileKey: String): String {
        return if (Files.exists(localFile(fileKey))) getLocalFileUrl(fileKey) else getBosFileUrl(fileKey)
    }

    override fun uploadBytes(data: ByteArray, fileKey: String, contentType: String): FileUploadResponse {
        val fileUrl = try {
            val metadata = ObjectMetadata()
            metadata.contentType = contentType
            metadata.contentLength = data.size.toLong()

            bosClient.putObject(
                bosProperties.bucketName,
                fileKey,
                data.inputStream(),
                metadata
            )

            getBosFileUrl(fileKey)
        } catch (e: Exception) {
            uploadBytesToLocal(data, fileKey)
        }

        return FileUploadResponse(
            fileKey = fileKey,
            fileUrl = fileUrl,
            fileName = fileKey.substringAfterLast("/"),
            fileSize = data.size.toLong()
        )
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

        validateMagicBytes(file, allowedTypes, fileTypeLabel)

        if (file.size > maxSize) {
            val maxSizeMB = maxSize / (1024 * 1024)
            throw IllegalArgumentException("${fileTypeLabel}大小不能超过 ${maxSizeMB}MB")
        }
    }

    /**
     * Validate file by magic bytes to prevent MIME type spoofing
     */
    private fun validateMagicBytes(file: MultipartFile, allowedTypes: Set<String>, fileTypeLabel: String) {
        val header = file.inputStream.use { it.readNBytes(8) }
        if (header.size < 4) return

        val detectedFormats = mutableListOf<String>()

        when {
            header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() -> detectedFormats.add("image/jpeg")
            header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() -> detectedFormats.add("image/png")
            header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x38.toByte() -> detectedFormats.add("image/gif")
            header[0] == 0x42.toByte() && header[1] == 0x4D.toByte() -> detectedFormats.add("image/bmp")
            header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte() -> detectedFormats.add("image/webp")
            header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte() -> detectedFormats.add("application/pdf")
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte() -> detectedFormats.add("application/zip")
        }

        if (detectedFormats.isNotEmpty()) {
            val hasMatch = detectedFormats.any { fmt ->
                when (fmt) {
                    "image/jpeg" -> allowedTypes.contains("image/jpeg") || allowedTypes.contains("image/jpg")
                    "application/zip" -> allowedTypes.contains("application/zip") || allowedTypes.any { it.contains("officedocument") || it.contains("opendocument") }
                    else -> allowedTypes.contains(fmt)
                }
            }
            if (!hasMatch) {
                throw IllegalArgumentException("${fileTypeLabel}文件实际格式与声明的类型不符，可能存在安全风险")
            }
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

    private fun storeFile(file: MultipartFile, fileKey: String, preferLocal: Boolean = false): String {
        if (preferLocal) {
            uploadToLocal(file, fileKey)
            return getLocalFileUrl(fileKey)
        }

        return try {
            uploadToBos(file, fileKey)
            getBosFileUrl(fileKey)
        } catch (e: Exception) {
            uploadToLocal(file, fileKey)
            getLocalFileUrl(fileKey)
        }
    }

    private fun getBosFileUrl(fileKey: String): String {
        return "https://${bosProperties.bucketName}.${bosProperties.endpoint}/${fileKey}"
    }

    private fun getLocalFileUrl(fileKey: String): String {
        val normalizedKey = fileKey.replace("\\", "/")
        return runCatching {
            ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(normalizedKey)
                .toUriString()
        }.getOrElse { "/uploads/$normalizedKey" }
    }

    private fun localRoot(): Path {
        return Paths.get(uploadPath).toAbsolutePath().normalize()
    }

    private fun localFile(fileKey: String): Path {
        val root = localRoot()
        val target = root.resolve(fileKey).normalize()
        if (!target.startsWith(root)) {
            throw IllegalArgumentException("Invalid file key")
        }
        return target
    }

    private fun uploadToLocal(file: MultipartFile, fileKey: String) {
        val target = localFile(fileKey)
        Files.createDirectories(target.parent)
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun uploadBytesToLocal(data: ByteArray, fileKey: String): String {
        val target = localFile(fileKey)
        Files.createDirectories(target.parent)
        Files.write(target, data)
        return getLocalFileUrl(fileKey)
    }

    private fun deleteLocalFile(fileKey: String): Boolean {
        return runCatching { Files.deleteIfExists(localFile(fileKey)) }.getOrDefault(false)
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
