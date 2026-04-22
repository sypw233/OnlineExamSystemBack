package ovo.sypw.onlineexamsystemback.service

import ovo.sypw.onlineexamsystemback.dto.request.ResetPasswordRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserCreateRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserUpdateRequest
import ovo.sypw.onlineexamsystemback.dto.response.BatchDeleteResult
import ovo.sypw.onlineexamsystemback.dto.response.UserResponse
import org.springframework.data.domain.Page

interface UserManagementService {

    /**
     * 分页查询用户列表, 支持角色、状态、关键字过滤
     */
    fun getUsers(
        role: String?,
        status: Int?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<UserResponse>

    /**
     * 根据ID查询用户详情
     */
    fun getUserById(id: Long): UserResponse

    /**
     * 管理员创建新用户(可以创建任意角色包括admin)
     */
    fun createUser(request: UserCreateRequest): UserResponse

    /**
     * 更新用户信息(角色、真实姓名、邮箱、状态)
     */
    fun updateUser(id: Long, request: UserUpdateRequest): UserResponse

    /**
     * 删除用户
     */
    fun deleteUser(id: Long)

    /**
     * 重置用户密码
     */
    fun resetPassword(id: Long, request: ResetPasswordRequest)

    /**
     * 启用或禁用用户账号
     */
    fun toggleUserStatus(id: Long, enable: Boolean): UserResponse

    /**
     * 按角色批量查询用户
     */
    fun getUsersByRole(role: String): List<UserResponse>

    /**
     * 批量删除用户
     */
    fun batchDelete(ids: List<Long>, currentUserId: Long): BatchDeleteResult
}
