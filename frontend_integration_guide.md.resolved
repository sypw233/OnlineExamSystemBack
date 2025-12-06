# 前端集成指南 (Frontend Integration Guide)

本文档旨在为前端开发者提供在线考试系统后端的集成指南，包含项目介绍、功能列表及API调用说明。

## 1. 项目介绍 (Project Introduction)

**在线考试系统 (Online Exam System)** 是一个基于 **Spring Boot 4.0** 和 **Kotlin** 开发的现代化考试平台后端。系统采用分层架构设计，支持多种角色的用户（学生、教师、管理员），并集成了 **AI辅助判题**、**防作弊监考** 等高级功能。

### 技术栈
- **后端框架**: Spring Boot 4.0.0
- **开发语言**: Kotlin (JVM)
- **数据库**: PostgreSQL
- **API文档**: OpenAPI 3
- **安全认证**: JWT (JSON Web Token)

## 2. 功能列表 (Features)

系统主要包含以下核心功能模块：

### 2.1 用户与权限
- **多角色支持**: 学生、教师、管理员。
- **注册登录**: 支持账号密码注册/登录，JWT Token 认证。
- **个人信息**: 查看及更新个人资料。

### 2.2 考试管理
- **考试创建**: 教师/管理员可创建考试，设置时间、时长。
- **监考设置**: 支持严格模式、全屏强制、设备限制（桌面/移动端）、切屏次数限制。
- **考试发布**: 草稿 -> 发布 -> 结束 的状态流转。
- **我的考试**: 学生查看待参加和已结束的考试。

### 2.3 题库与题目
- **题库管理**: 分类管理题目。
- **多种题型**: 单选题、多选题、判断题、填空题、简答题。
- **题目导入**: 支持 Excel/Word 批量导入题目。

### 2.4 答题与评分
- **在线答题**: 学生在线作答，支持自动保存。
- **自动评分**: 客观题（单选/多选/判断）系统自动评分。
- **人工/AI评分**: 主观题支持教师人工评分，或使用 AI 辅助评分。

### 2.5 课程管理
- **课程发布**: 教师创建和管理课程。
- **学生选课**: 学生加入课程，关联考试。

## 3. API 集成指南 (API Integration)

### 3.1 基础信息
- **接口前缀**: `/api`
- **数据格式**: JSON
- **统一响应结构**:
  ```typescript
  interface Result<T> {
    code: number;       // 200 表示成功，其他为错误码
    message: string;    // 提示信息
    data: T | null;     // 业务数据
  }
  ```

### 3.2 认证流程 (Authentication)

系统使用 JWT 进行无状态认证。

1.  **登录**: 调用 `POST /api/auth/login` 获取 `accessToken` 和 [refreshToken](file:///f:/IdeaProjects/OnlineExamSystemBack/src/main/kotlin/ovo/sypw/onlineexamsystemback/controller/AuthController.kt#106-134)。
2.  **请求头**: 在所有受保护的接口请求头中携带 Token：
    ```
    Authorization: Bearer <accessToken>
    ```
3.  **Token 刷新**: 当接口返回 `401` 时，使用 [refreshToken](file:///f:/IdeaProjects/OnlineExamSystemBack/src/main/kotlin/ovo/sypw/onlineexamsystemback/controller/AuthController.kt#106-134) 调用 `/api/auth/refresh` 换取新 Token。

### 3.3 核心接口概览

#### 用户相关
- `POST /api/auth/register`: 用户注册
- `POST /api/auth/login`: 用户登录
- `GET /api/auth/me`: 获取当前用户信息

#### 考试相关
- `GET /api/exams`: 获取考试列表
- `GET /api/exams/{id}`: 获取考试详情（含监考配置）
- `POST /api/exams`: 创建考试（教师/管理员）
- `POST /api/exams/{id}/publish`: 发布考试
- `GET /api/exams/my`: 获取我的考试列表

#### 题目相关
- `POST /api/questions`: 创建题目
- `POST /api/questions/import`: 批量导入题目

#### 答题相关
- `POST /api/submissions`: 提交试卷
- `GET /api/submissions/{id}`: 查看答题详情

### 3.4 枚举字典

- **用户角色 (Role)**: `admin` (管理员), `teacher` (教师), `student` (学生)
- **考试状态**: `0` (草稿), `1` (已发布), `2` (已结束)
- **题型**: `single` (单选), `multiple` (多选), `true_false` (判断), `fill_blank` (填空), `short_answer` (简答)

## 4. 开发建议

- **API 文档**: 访问 `/swagger-ui.html` 查看完整的接口定义和调试。
- **时间格式**: 所有时间字段通常为 ISO 8601 格式字符串。
- **错误处理**: 请统一处理非 200 的状态码，并展示 `message` 字段给用户。
