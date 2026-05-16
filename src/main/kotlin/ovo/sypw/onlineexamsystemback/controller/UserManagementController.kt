package ovo.sypw.onlineexamsystemback.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ovo.sypw.onlineexamsystemback.dto.request.BatchDeleteRequest
import ovo.sypw.onlineexamsystemback.dto.request.ResetPasswordRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserCreateRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserUpdateRequest
import ovo.sypw.onlineexamsystemback.dto.response.UserResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.extensions.safeId
import ovo.sypw.onlineexamsystemback.security.CurrentUser
import ovo.sypw.onlineexamsystemback.service.UserManagementService
import ovo.sypw.onlineexamsystemback.util.Result
import org.springframework.data.domain.Page
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "用户管理(管理员)", description = "管理员对用户账户的增删改查及权限管理接口")
@SecurityRequirement(name = "Bearer Authentication")
class UserManagementController(
    private val userManagementService: UserManagementService
) {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(
        summary = "分页查询用户列表",
        description = "管理员可按角色、状态、关键字筛选, 支持分页"
    )
    fun getUsers(
        @Parameter(description = "角色过滤: admin/teacher/student") @RequestParam(required = false) role: String?,
        @Parameter(description = "状态过滤: 1-启用, 0-禁用") @RequestParam(required = false) status: Int?,
        @Parameter(description = "关键字搜索(匹配用户名、真实姓名、邮箱)") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "页码, 从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") size: Int
    ): Result<Page<UserResponse>> {
        val result = userManagementService.getUsers(role, status, keyword, page, size)
        return Result.success(result)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户ID获取详细信息")
    fun getUserById(
        @Parameter(description = "用户ID") @PathVariable id: Long
    ): Result<UserResponse> {
        return try {
            Result.success(userManagementService.getUserById(id))
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "用户不存在", 404)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "新建用户", description = "管理员创建新用户, 可指定任意角色(包括admin)")
    fun createUser(
        @Valid @RequestBody request: UserCreateRequest
    ): Result<UserResponse> {
        return try {
            Result.success(userManagementService.createUser(request), "用户创建成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "创建失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(
        summary = "更新用户信息",
        description = "修改用户的役名、邮箱、角色或状态, 所有字段均为可选"
    )
    fun updateUser(
        @Parameter(description = "用户ID") @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): Result<UserResponse> {
        return try {
            Result.success(userManagementService.updateUser(id, request), "用户信息更新成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "更新失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "管理员删除指定用户, 无法删除自己")
    fun deleteUser(
        @CurrentUser user: User,
        @Parameter(description = "用户ID") @PathVariable id: Long
    ): Result<String> {
        if (user.id == id) {
            return Result.error("不能删除自己的账号", 400)
        }
        return try {
            userManagementService.deleteUser(id)
            Result.success("用户删除成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "删除失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置指定用户的密码")
    fun resetPassword(
        @Parameter(description = "用户ID") @PathVariable id: Long,
        @Valid @RequestBody request: ResetPasswordRequest
    ): Result<String> {
        return try {
            userManagementService.resetPassword(id, request)
            Result.success("密码重置成功")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "密码重置失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用用户账号", description = "管理员启用被禁用的用户账号")
    fun enableUser(
        @Parameter(description = "用户ID") @PathVariable id: Long
    ): Result<UserResponse> {
        return try {
            Result.success(userManagementService.toggleUserStatus(id, true), "账号已启用")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "操作失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户账号", description = "管理员禁用指定用户账号, 无法禁用自己")
    fun disableUser(
        @CurrentUser user: User,
        @Parameter(description = "用户ID") @PathVariable id: Long
    ): Result<UserResponse> {
        if (user.id == id) {
            return Result.error("不能禁用自己的账号", 400)
        }
        return try {
            Result.success(userManagementService.toggleUserStatus(id, false), "账号已禁用")
        } catch (e: IllegalArgumentException) {
            Result.error(e.message ?: "操作失败", 400)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch-delete")
    @Operation(
        summary = "批量删除用户",
        description = "管理员批量删除指定用户。删除失败会继续处理后续记录，不会删除当前登录用户。",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    fun batchDeleteUsers(
        @Valid @RequestBody request: BatchDeleteRequest,
        @CurrentUser user: User
    ): Result<ovo.sypw.onlineexamsystemback.dto.response.BatchDeleteResult> {
        val result = userManagementService.batchDelete(request.ids, user.safeId)
        return Result.success(result, "批量删除完成：成功 ${result.successCount} 条，失败 ${result.failedCount} 条")
    }

}
