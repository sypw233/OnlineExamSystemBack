# AGENTS.md

## Build & Run

```bash
./gradlew bootRun        # start dev server
./gradlew bootJar        # build fat JAR
./gradlew test           # run tests
./gradlew build          # compile + test + assemble
```

Requires JDK 21+ and a running PostgreSQL instance on `localhost:5432/exam_system` (see `src/main/resources/application.properties` for creds). JPA `ddl-auto=update` so schema auto-migrates — but the root `schema.sql` has the canonical DDL and sample data. Additional SQL migration files at root (`migration_*.sql`) must be applied manually for added features (proctoring fields, AI grading, notifications).

## Architecture

Single-module Spring Boot 4.0 + Kotlin backend. Package `ovo.sypw.onlineexamsystemback`:

- `controller/` — REST controllers, all under `/api/**`. Unified response type `Result<T>` (code, message, data).
- `service/` — interfaces; implementations in `service/impl/`.
- `repository/` — Spring Data JPA repositories (no custom queries in most).
- `entity/` — JPA entities, all annotated `@Entity`. Uses `@allOpen` plugin via `build.gradle.kts` for entity/mapped-superclass/embeddable.
- `dto/request/` and `dto/response/` — request/response DTOs.
- `config/` — SecurityConfig (JWT + stateless sessions), SwaggerConfig (OpenAPI 3), BosConfig, WebConfig, JacksonConfig.
- `security/` — `JwtTokenProvider`, `JwtAuthenticationFilter`, `UserDetailsServiceImpl`.
- `enums/` — `QuestionType`, `ExamStatus`, `DifficultyLevel`, `NotificationType`, `ExamPlatform`.
- `exception/GlobalExceptionHandler` — maps Spring Security + validation errors to `Result`.

## Auth & Security

JWT-based, stateless. Only `/api/auth/**` and Swagger endpoints are unauthenticated (`SecurityConfig`). All other endpoints require a Bearer token. Roles are plain strings: `admin`, `teacher`, `student`.

Swagger UI at `/swagger-ui.html`. Bearer token scheme is pre-configured.

## API Response Convention

Every endpoint returns `Result<T>` — `{code, message, data}`. Success is 200; errors use appropriate HTTP codes (400, 401, 403, 500). Controllers should return `Result.success(data)` / `Result.error(msg, code)`.

## Question Types (enum values)

`single`, `multiple`, `true_false`, `fill_blank`, `short_answer` — stored as strings, validated via `QuestionType` enum.

## Exam Status Codes

`0`=Draft, `1`=Published, `2`=Ended (integer field on `exams.status`).

## Key External Integrations

- **Baidu Cloud BOS** — file storage (config in `BosConfig`, env vars `BOS_ACCESS_KEY_ID`, etc.).
- **OpenAI API** — AI grading (`AiGradingService`), key via `OPENAI_API_KEY` env var.
- **Apache POI** — Excel import/export for questions.

## Testing

One smoke test exists (`OnlineExamSystemBackApplicationTests.contextLoads`). Use JUnit 5 (Jupiter) via `./gradlew test`. No integration test containers configured — tests will fail without a live PostgreSQL connection unless `@SpringBootTest` is avoided or a test profile overrides the datasource.

## Code Conventions

- Kotlin data classes for entities (not plain classes). Lombok-style equals/hashCode via data class.
- `@Transactional` at service class level, not method level (see `ExamServiceImpl`).
- Constructors via DI — no field injection.
- Validation via Jakarta Bean Validation (`@Valid` on request bodies).
- Chinese-language strings in error messages and API documentation tags.
