# SERVICE LAYER

**Scope:** `service/` — 11 interfaces + 11 implementations + 2 scheduled jobs

## STRUCTURE

```
service/
├── AiGradingService.kt / impl/AiGradingServiceImpl.kt
├── CourseService.kt / impl/CourseServiceImpl.kt
├── ExamService.kt / impl/ExamServiceImpl.kt
├── FileService.kt / impl/FileServiceImpl.kt
├── NotificationService.kt / impl/NotificationServiceImpl.kt
├── QuestionBankService.kt / impl/QuestionBankServiceImpl.kt
├── QuestionImportExportService.kt / impl/QuestionImportExportServiceImpl.kt
├── QuestionService.kt / impl/QuestionServiceImpl.kt
├── StatisticsService.kt / impl/StatisticsServiceImpl.kt
├── SubmissionService.kt / impl/SubmissionServiceImpl.kt
├── UserManagementService.kt / impl/UserManagementServiceImpl.kt
└── scheduled/
    ├── ExamScheduledService.kt          # Auto-expire exams (hourly cron)
    └── NotificationScheduledService.kt  # Exam reminders + cleanup
```

## PATTERN

```kotlin
// Interface
interface XxxService {
    fun getXxx(id: Long): XxxResponse
    fun createXxx(request: XxxRequest, userId: Long): XxxResponse
}

// Implementation
@Service
@Transactional
class XxxServiceImpl(
    private val xxxRepository: XxxRepository,
    private val yyyRepository: YyyRepository
) : XxxService {

    override fun getXxx(id: Long): XxxResponse {
        val entity = xxxRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("XXX不存在")
        }
        return entity.toResponse()
    }
}
```

## CONVENTIONS

- **Interface + Impl**: Every service has an interface in `service/` and implementation in `service/impl/`.
- **`@Transactional`**: At **class level**, not method level. All methods are transactional by default.
- **Constructor injection**: Inject repositories and other services via constructor. No `@Autowired`.
- **Error handling**: Throw `IllegalArgumentException` for business validation errors. `GlobalExceptionHandler` maps to `Result.error()`.
- **Entity lookup**: Use `repository.findById(id).orElseThrow { throw IllegalArgumentException("...") }`.
- **DTO mapping**: Services return response DTOs. Use manual mapping or extension functions (no MapStruct).
- **Pagination**: Use `Pageable` param, return `Page<XxxResponse>`.
- **Chinese errors**: Error messages in Chinese — `"用户不存在"`, `"课程不存在"`, etc.

## SCHEDULED JOBS

| Service | Cron | Purpose |
|---------|------|---------|
| `ExamScheduledService` | `0 0 * * * *` (hourly) | Auto-expire exams past `endTime` |
| `NotificationScheduledService` | Multiple | 24h/1h exam reminders, daily cleanup |

## ANTI-PATTERNS

| Forbidden | Fix |
|-----------|-----|
| `@Transactional` on methods only | Put on class |
| Returning entities from service methods | Map to `*Response` |
| Catching exceptions silently | Let `GlobalExceptionHandler` handle |
| Direct repository access from controller | Always go through service |
| Business logic in entity | Move to service |
| `@Autowired` field injection | Constructor parameter |

## HOTSPOTS

- `ExamServiceImpl.kt` (885 lines) — Largest service. Complex exam composition logic.
- `SubmissionServiceImpl.kt` — 3 `@Suppress("UNCHECKED_CAST")` — type safety gaps.
- `AiGradingServiceImpl.kt` — 4 `@Suppress("UNCHECKED_CAST")` — type safety gaps.
