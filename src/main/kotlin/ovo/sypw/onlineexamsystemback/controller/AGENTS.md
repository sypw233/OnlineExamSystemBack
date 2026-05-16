# CONTROLLER LAYER

**Scope:** `controller/` — 12 REST controllers under `/api/**`

## STRUCTURE

```
controller/
├── AuthController.kt              # /api/auth/** — login, register, refresh, profile
├── CourseController.kt            # /api/courses/**
├── ExamController.kt              # /api/exams/**
├── QuestionController.kt          # /api/questions/**
├── QuestionBankController.kt      # /api/question-banks/**
├── QuestionImportExportController.kt  # /api/questions/import, /api/questions/export
├── SubmissionController.kt        # /api/submissions/**
├── AiGradingController.kt         # /api/ai-grading/**
├── FileController.kt              # /api/files/**
├── NotificationController.kt      # /api/notifications/**
├── StatisticsController.kt        # /api/statistics/**
└── UserManagementController.kt    # /api/users/**
```

## PATTERN

```kotlin
@RestController
@RequestMapping("/api/{domain}")
@Tag(name = "中文名称", description = "中文描述")
class XxxController(
    private val xxxService: XxxService   // constructor injection
) {
    @GetMapping("/{id}")
    @Operation(summary = "中文摘要", description = "中文详细描述")
    fun getXxx(@PathVariable id: Long): Result<XxxResponse> {
        return Result.success(xxxService.getXxx(id))
    }
}
```

## CONVENTIONS

- **Return type**: Always `Result<T>`. Never return raw entities.
- **Injection**: Constructor injection of service interfaces. No `@Autowired`.
- **Auth**: Use `@CurrentUser user: User` to get authenticated user.
- **Validation**: `@Valid @RequestBody` on POST/PUT params.
- **Swagger**: `@Tag` on class, `@Operation` on methods. Chinese descriptions.
- **Roles**: `@PreAuthorize("hasRole('ADMIN')")` or `@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")`.
- **Path style**: kebab-case (`/api/question-banks`), not camelCase.

## ANTI-PATTERNS

| Forbidden | Fix |
|-----------|-----|
| Returning entity directly | Map to `*Response` DTO |
| Business logic in controller | Delegate to service |
| `@Autowired` field injection | Constructor parameter |
| Missing `@Valid` on request body | Add `@Valid @RequestBody` |
| Missing `@Operation` Swagger annotation | Add `@Operation(summary, description)` |
