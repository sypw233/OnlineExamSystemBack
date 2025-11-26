package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.response.FileUploadResponse
import org.springframework.web.multipart.MultipartFile

interface FileService {
    /**
     * Upload image file
     * @param file Image file
     * @param category Category (questions/avatars)
     * @param userId Uploader ID
     * @return Upload response with file URL
     */
    fun uploadImage(file: MultipartFile, category: String, userId: Long): FileUploadResponse
    
    /**
     * Upload document file
     * @param file Document file
     * @param category Category (attachments/materials)
     * @param userId Uploader ID
     * @return Upload response with file URL
     */
    fun uploadDocument(file: MultipartFile, category: String, userId: Long): FileUploadResponse
    
    /**
     * Delete file from BOS
     * @param fileKey File key in BOS
     * @param userId User ID (for permission check)
     */
    fun deleteFile(fileKey: String, userId: Long, userRole: String)
    
    /**
     * Get file public URL
     * @param fileKey File key in BOS
     * @return Public URL
     */
    fun getFileUrl(fileKey: String): String
}
