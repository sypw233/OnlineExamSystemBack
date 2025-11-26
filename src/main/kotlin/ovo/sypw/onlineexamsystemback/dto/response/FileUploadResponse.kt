package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "文件上传响应")
data class FileUploadResponse(
    @Schema(description = "文件在BOS中的key", example = "images/questions/20241126/abc123.jpg")
    val fileKey: String,
    
    @Schema(description = "文件访问URL", example = "https://your-bucket.bj.bcebos.com/images/questions/20241126/abc123.jpg")
    val fileUrl: String,
    
    @Schema(description = "原始文件名", example = "question-image.jpg")
    val fileName: String,
    
    @Schema(description = "文件大小（字节）", example = "102400")
    val fileSize: Long
)
