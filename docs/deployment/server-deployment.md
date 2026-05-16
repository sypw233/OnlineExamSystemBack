# Server Deployment

Target server: `106.13.185.188`

Runtime:

- Ubuntu 24.04
- OpenJDK 21 runtime
- PostgreSQL 16
- Spring Boot jar deployed at `/opt/online-exam-system/app.jar`
- systemd service: `online-exam-system.service`

Required environment variables in systemd:

- `DB_URL=jdbc:postgresql://localhost:5432/exam_system`
- `DB_USERNAME=postgres`
- `DB_PASSWORD`
- `JWT_SECRET`
- optional BOS and OpenAI-compatible API variables when not stored in `ai_config`

Operational commands:

```bash
systemctl status online-exam-system
systemctl restart online-exam-system
journalctl -u online-exam-system -f
PGPASSWORD=... psql -h localhost -U postgres -d exam_system
```

The May 16, 2026 test deployment recreated all application data and loaded:

- 1 admin account
- 10 teacher accounts
- 200 student accounts
- 10 courses
- 15 question banks
- 1500 questions
- 10 published exams
- 200 submissions
- notification and AI grading configuration data

All test account passwords are `123456`. Fixed accounts are `admin`, `teacher1`, and `student`.
