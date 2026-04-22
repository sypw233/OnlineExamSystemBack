package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.BatchDeleteRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamQuestionRequest
import ovo.sypw.onlineexamsystemback.dto.request.ExamRequest
import ovo.sypw.onlineexamsystemback.dto.response.ExamQuestionResponse
import ovo.sypw.onlineexamsystemback.dto.response.ExamResponse
import ovo.sypw.onlineexamsystemback.dto.response.SubmissionResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.CourseRepository
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.ExamService
import ovo.sypw.onlineexamsystemback.service.SubmissionService
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "考试相关接口")
class ExamController(
    private val examService: ExamService,
    private val submissionService: SubmissionService,
    private val courseRepository: CourseRepository
) {

    @PostMapping
    @Operation(
        summary = "创建考试",
        description = """
            教师和管理员创建新考试
            
            监考控制功能:
            - allowedPlatforms: 限制考试平台 (desktop/mobile/both)
            - strictMode: 开启严格监考
            - maxSwitchCount: 最大切出次数
            - fullscreenRequired: 强制全屏（仅桌面端）
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun createExam(
        @CurrentUser user: User,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "考试信息",
            required = true
        )
        @Valid @RequestBody request: ExamRequest
    ): Result<ExamResponse> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以创建考试", 403)
        }

        return try {
            val exam = examService.createExam(request, user.safeId)
            Result.success(exam, "考试创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @GetMapping
    @Operation(
        summary = "查询考试列表",
        description = "管理员获取全部考试，教师获取自己授课课程的考试。支持按状态、课程筛选，支持分页。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getAllExams(
        @CurrentUser user: User,
        @Parameter(description = "页码", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "考试状态", schema = Schema(allowableValues = ["0", "1", "2"]))
        @RequestParam(required = false) status: Int?,
        @Parameter(description = "课程ID")
        @RequestParam(required = false) courseId: Long?
    ): Result<Page<ExamResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100))

        // If teacher, limit by their teaching courses
        if (user.role == "teacher") {
            val courseIds = courseRepository.findByTeacherId(user.safeId).map { it.id ?: 0L }
            if (courseIds.isEmpty()) {
                return Result.success(Page.empty(pageable))
            }
            if (courseId != null && courseId !in courseIds) {
                return Result.error("您没有权限查看此课程的考试", 403)
            }
            val exams = if (courseId != null) {
                examService.getExamsByCourse(courseId, pageable)
            } else {
                examService.getMyTeachingExams(user.safeId, pageable)
            }
            // In-memory status filter for teachers (page metadata may be slightly off, acceptable for small datasets)
            val filtered = if (status != null) {
                val content = exams.content.filter { it.status == status }
                org.springframework.data.domain.PageImpl(content, exams.pageable, content.size.toLong())
            } else exams
            return Result.success(filtered)
        }

        // Admin: use unified search
        val exams = examService.searchExams(
            creatorId = null,
            status = status,
            courseId = courseId,
            pageable = pageable
        )
        return Result.success(exams)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取考试详情",
        description = "根据ID获取考试详细信息（包含监考设置）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamById(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<ExamResponse> {
        return try {
            val exam = examService.getExamById(id)
            Result.success(exam)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "考试不存在", 404)
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新考试",
        description = "教师更新自己的考试，管理员可更新任何考试。只有草稿状态的考试可以修改。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun updateExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: ExamRequest
    ): Result<ExamResponse> {
        return try {
            val exam = examService.updateExam(id, request, user.safeId, user.role)
            Result.success(exam, "考试更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除考试",
        description = "教师删除自己的考试，管理员可删除任何考试",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun deleteExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<String> {
        return try {
            examService.deleteExam(id, user.safeId, user.role)
            Result.success("考试删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @PostMapping("/batch-delete")
    @Operation(
        summary = "批量删除考试",
        description = "教师批量删除自己的考试，管理员可删除任何考试。删除失败会继续处理后续记录。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun batchDeleteExams(
        @Valid @RequestBody request: BatchDeleteRequest,
        @CurrentUser user: User
    ): Result<ovo.sypw.onlineexamsystemback.dto.response.BatchDeleteResult> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以批量删除考试", 403)
        }
        val result = examService.batchDelete(request.ids, user.safeId, user.role)
        return Result.success(result, "批量删除完成：成功 ${result.successCount} 条，失败 ${result.failedCount} 条")
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "部分更新考试",
        description = "支持修改考试状态（如发布草稿考试）。教师只能修改自己的考试，管理员可修改任何考试。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun patchExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Parameter(description = "状态: 1=发布", schema = Schema(allowableValues = ["1"]))
        @RequestParam status: Int
    ): Result<ExamResponse> {
        return try {
            val exam = examService.patchExam(id, status, user.safeId, user.role)
            Result.success(exam, "考试更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @PostMapping("/{id}/submissions")
    @Operation(
        summary = "开始考试",
        description = "学生开始考试，创建答题记录。如果已经开始过，返回现有记录。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun startExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<SubmissionResponse> {
        if (user.role != "student") {
            return Result.error("只有学生可以开始考试", 403)
        }

        return try {
            val submission = submissionService.startExam(id, user.safeId)
            Result.success(submission, "考试已开始")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "开始失败", 400)
        }
    }

    @PostMapping("/{id}/questions")
    @Operation(
        summary = "添加题目到考试",
        description = """
            将指定题目添加到考试中（仅草稿状态）
            
            注意:
            - 需要指定题目分值和显示顺序
            - 不能添加重复题目
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun addQuestionToExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: ExamQuestionRequest
    ): Result<String> {
        return try {
            examService.addQuestionToExam(id, request, user.safeId, user.role)
            Result.success("题目添加成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "添加失败", 400)
        }
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(
        summary = "从考试移除题目",
        description = "将指定题目从考试中移除（仅草稿状态）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun removeQuestionFromExam(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long,
        @Parameter(description = "题目ID", example = "1") @PathVariable questionId: Long
    ): Result<String> {
        return try {
            examService.removeQuestionFromExam(id, questionId, user.safeId, user.role)
            Result.success("题目移除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "移除失败", 400)
        }
    }

    @GetMapping("/{id}/questions")
    @Operation(
        summary = "获取考试的所有题目",
        description = "教师和管理员查看指定考试包含的所有题目（按顺序排列），学生无权访问",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamQuestions(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<List<ExamQuestionResponse>> {
        if (user.role != "teacher" && user.role != "admin") {
            return Result.error("只有教师和管理员可以查看考试题目", 403)
        }

        return try {
            val questions = examService.getExamQuestions(id)
            Result.success(questions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "查询失败", 400)
        }
    }

    @GetMapping("/my-available")
    @Operation(
        summary = "获取待考考试列表",
        description = "学生获取当前可以参加的考试列表（已发布、时间有效、尚未提交）",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyAvailableExams(
        @CurrentUser user: User,
        @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") size: Int
    ): Result<Page<ExamResponse>> {
        if (user.role != "student") {
            return Result.error("只有学生可以查看待考列表", 403)
        }

        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val exams = examService.getStudentAvailableExams(user.safeId, pageable)
        return Result.success(exams)
    }

    @GetMapping("/my-completed")
    @Operation(
        summary = "获取已考/已结束考试列表",
        description = "学生获取已提交或已结束的考试列表",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getMyCompletedExams(
        @CurrentUser user: User,
        @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") size: Int
    ): Result<Page<ExamResponse>> {
        if (user.role != "student") {
            return Result.error("只有学生可以查看已考列表", 403)
        }

        val pageable = PageRequest.of(page, size.coerceAtMost(100))
        val exams = examService.getStudentCompletedExams(user.safeId, pageable)
        return Result.success(exams)
    }

    @GetMapping("/{id}/paper")
    @Operation(
        summary = "获取考试试卷（学生）",
        description = """
            学生获取考试试卷题目列表（不含答案和解析）
            
            ## 权限与限制
            - 仅学生可访问
            - 考试必须已发布且在当前时间内
            - 学生必须已选修该课程
            
            ## 返回数据
            - 包含题目内容、类型、难度、选项、分值和顺序
            - 不含标准答案和解析
        """,
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun getExamPaper(
        @CurrentUser user: User,
        @Parameter(description = "考试ID", example = "1") @PathVariable id: Long
    ): Result<List<ovo.sypw.onlineexamsystemback.dto.response.ExamPaperQuestionResponse>> {
        if (user.role != "student") {
            return Result.error("只有学生可以获取考试试卷", 403)
        }

        return try {
            val questions = examService.getExamPaper(id, user.safeId)
            Result.success(questions)
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "获取试卷失败", 400)
        }
    }
}
