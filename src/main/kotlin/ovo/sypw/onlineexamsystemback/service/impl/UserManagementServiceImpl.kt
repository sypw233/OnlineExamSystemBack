package ovo.sypw.onlineexamsystemback.service.impl

import ovo.sypw.onlineexamsystemback.dto.request.ResetPasswordRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserCreateRequest
import ovo.sypw.onlineexamsystemback.dto.request.UserUpdateRequest
import ovo.sypw.onlineexamsystemback.dto.response.UserResponse
import ovo.sypw.onlineexamsystemback.entity.User
import ovo.sypw.onlineexamsystemback.repository.*
import ovo.sypw.onlineexamsystemback.service.UserManagementService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserManagementServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val examSubmissionRepository: ExamSubmissionRepository,
    private val courseRepository: CourseRepository,
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository,
    private val questionBankRepository: QuestionBankRepository
) : UserManagementService {

    private fun User.toResponse() = UserResponse(
        id = this.id ?: 0L,
        username = this.username,
        nickname = this.nickname,
        realName = this.realName,
        role = this.role,
        email = this.email,
        avatar = this.avatar,
        status = this.status,
        createTime = this.createTime
    )

    override fun getUsers(
        role: String?,
        status: Int?,
        keyword: String?,
        page: Int,
        size: Int
    ): Page<UserResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
        return userRepository.searchUsers(
            role = role,
            status = status,
            keyword = keyword?.takeIf { it.isNotBlank() },
            pageable = pageable
        ).map { it.toResponse() }
    }

    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            IllegalArgumentException("用户不存在, id=$id")
        }
        return user.toResponse()
    }

    @Transactional
    override fun createUser(request: UserCreateRequest): UserResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("用户名 '${request.username}' 已存在")
        }
        request.email?.let { email ->
            if (userRepository.existsByEmail(email)) {
                throw IllegalArgumentException("邮箱 '$email' 已被注册")
            }
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password) ?: "",
            realName = request.realName,
            role = request.role,
            email = request.email,
            status = 1
        )
        return userRepository.save(user).toResponse()
    }

    @Transactional
    override fun updateUser(id: Long, request: UserUpdateRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            IllegalArgumentException("用户不存在, id=$id")
        }

        request.email?.let { newEmail ->
            val existing = userRepository.findByEmail(newEmail)
            if (existing != null && existing.id != id) {
                throw IllegalArgumentException("邮箱 '$newEmail' 已被其他用户使用")
            }
        }

        request.realName?.let { user.realName = it }
        request.email?.let { user.email = it }
        request.role?.let { user.role = it }
        request.status?.let { user.status = it }
        return userRepository.save(user).toResponse()
    }

    @Transactional
    override fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw IllegalArgumentException("用户不存在, id=$id")
        }

        // Check for associated records to prevent orphan data
        val submissionCount = examSubmissionRepository.findByUserId(id).size
        if (submissionCount > 0) {
            throw IllegalArgumentException("该用户有 $submissionCount 条考试提交记录，无法删除")
        }

        val courseCount = courseRepository.findByTeacherId(id).size
        if (courseCount > 0) {
            throw IllegalArgumentException("该用户创建了 $courseCount 门课程，无法删除")
        }

        val examCount = examRepository.findByCreatorId(id).size
        if (examCount > 0) {
            throw IllegalArgumentException("该用户创建了 $examCount 场考试，无法删除")
        }

        val questionCount = questionRepository.findByCreatorId(id).size
        if (questionCount > 0) {
            throw IllegalArgumentException("该用户创建了 $questionCount 道题目，无法删除")
        }

        val bankCount = questionBankRepository.findByCreatorId(id).size
        if (bankCount > 0) {
            throw IllegalArgumentException("该用户创建了 $bankCount 个题库，无法删除")
        }

        userRepository.deleteById(id)
    }

    @Transactional
    override fun resetPassword(id: Long, request: ResetPasswordRequest) {
        val user = userRepository.findById(id).orElseThrow {
            IllegalArgumentException("用户不存在, id=$id")
        }
        user.password = passwordEncoder.encode(request.newPassword) ?: ""
        userRepository.save(user)
    }

    @Transactional
    override fun toggleUserStatus(id: Long, enable: Boolean): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            IllegalArgumentException("用户不存在, id=$id")
        }
        val newStatus = if (enable) 1 else 0
        user.status = newStatus
        return userRepository.save(user).toResponse()
    }

    override fun getUsersByRole(role: String): List<UserResponse> {
        return userRepository.findByRole(role).map { it.toResponse() }
    }
}
