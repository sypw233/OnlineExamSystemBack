# BACKEND KNOWLEDGE BASE

**Project:** OnlineExamSystemBack
**Stack:** Spring Boot 4.0 + Kotlin 2.2 + JDK 21 + PostgreSQL

## BUILD & RUN

```bash
./gradlew bootRun        # start dev server (needs PostgreSQL)
./gradlew bootJar        # build fat JAR
./gradlew test           # run tests (needs live DB)
./gradlew build          # compile + test + assemble
```

Requires PostgreSQL on `localhost:5432/exam_system`. Schema auto-migrates via `ddl-auto=update`. Canonical DDL in `schema.sql`. Manual migration scripts: `migration_*.sql` at project root (NO Flyway/Liquibase).

## STRUCTURE

```
src/main/kotlin/ovo/sypw/onlineexamsystemback/
├── controller/     # 12 REST controllers under /api/**
├── service/        # 11 interfaces + impl/ (11 impls) + scheduled/ (2 jobs)
├── repository/     # 11 Spring Data JPA repos
├── entity/         # 11 JPA entities (allOpen plugin)
├── dto/request/    # 21 request DTOs
├── dto/response/   # 24 response DTOs
├── config/         # SecurityConfig, SwaggerConfig, BosConfig, WebConfig, JacksonConfig
├── security/       # JwtTokenProvider, JwtAuthenticationFilter, @CurrentUser
├── enums/          # QuestionType, ExamStatus, DifficultyLevel, NotificationType, ExamPlatform
├── exception/      # GlobalExceptionHandler
├── extensions/     # User.safeId extension
└── util/           # Result<T> wrapper
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add REST endpoint | `controller/` | See `controller/AGENTS.md` |
| Add business logic | `service/` → `service/impl/` | See `service/AGENTS.md` |
| Add JPA entity | `entity/` | Use `@allOpen` — no need for `open` keyword |
| Add repository | `repository/` | Extend `JpaRepository<T, Long>` |
| Add request DTO | `dto/request/` | Use `@Valid` annotations |
| Add response DTO | `dto/response/` | Mirror entity fields, exclude sensitive data |
| Add enum | `enums/` | String-stored, used in validation |
| Modify security | `config/SecurityConfig.kt` | JWT filter chain, public vs protected routes |
| Add scheduled job | `service/scheduled/` | `@Scheduled` cron expressions |

## CONVENTIONS

- **Response**: ALL endpoints return `Result<T>` — `{code, message, data}`. Use `Result.success(data)` / `Result.error(msg, code)`.
- **Auth**: Inject `@CurrentUser user: User` in controller params. JWT Bearer token in `Authorization` header.
- **Roles**: Plain strings — `admin`, `teacher`, `student`. Use `@PreAuthorize("hasRole('ADMIN')")`.
- **DI**: Constructor injection only. No `@Autowired`.
- **Transactions**: `@Transactional` at **class level** on service implementations.
- **Validation**: `@Valid` on `@RequestBody` params. Jakarta Bean Validation annotations on DTOs.
- **Errors**: Throw `IllegalArgumentException` for business errors — `GlobalExceptionHandler` maps to `Result`.
- **Language**: Chinese in error messages, Swagger `@Tag`/`@Operation` descriptions. English in code identifiers.
- **Entities**: Kotlin classes (NOT data classes) with `@allOpen` plugin. Manual `equals()`/`hashCode()` by `id`.
- **Naming**: Controllers `{Domain}Controller`, Services `{Domain}Service`/`{Domain}ServiceImpl`, DTOs `{Domain}Request`/`{Domain}Response`.

## ANTI-PATTERNS

| Forbidden | Why | Fix |
|-----------|-----|-----|
| `@Autowired` field injection | Project uses constructor injection | Declare deps in constructor |
| `@Transactional` on methods | Convention is class-level | Put on `ServiceImpl` class |
| Returning raw entities | Exposes internal model | Map to `*Response` DTOs |
| Using dev JWT secret in prod | Hardcoded `dev-only-secret-*` | Set `JWT_SECRET` env var |
| `spring.datasource.password=123456` default | Weak fallback in properties | Set `DB_PASSWORD` env var |

## SECURITY

- JWT stateless sessions. `/api/auth/**` + Swagger are public; all else requires Bearer token.
- **PRODUCTION**: MUST set env vars — `JWT_SECRET`, `DB_PASSWORD`, `BOS_ACCESS_KEY_ID`, `BOS_SECRET_ACCESS_KEY`, `OPENAI_API_KEY`.
- `ddl-auto=update` — consider switching to Flyway for production.

## EXTERNAL INTEGRATIONS

- **Baidu Cloud BOS** — file storage (`BosConfig`)
- **OpenAI API** — AI grading (`AiGradingService`)
- **Apache POI** — Excel import/export for questions

## TESTING

JUnit 5 + Mockito. Only 2 test files exist. No test profile — `@SpringBootTest` requires live PostgreSQL. JaCoCo configured for coverage reports.

## COMMANDS

```bash
./gradlew test                              # Run tests
./gradlew :jacocoTestReport                 # Generate coverage report
./gradlew bootRun --args='--spring.profiles.active=dev'  # Run with dev profile
```

## CHILD DOCS

- `controller/AGENTS.md` — Controller layer conventions
- `service/AGENTS.md` — Service layer conventions
