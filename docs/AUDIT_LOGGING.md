# Structured Action Audit Logging & Distributed Tracing

## 1. Overview & Architecture

Lingua Optima features enterprise-grade **Action Audit Logging** and **Distributed Tracing**. Every HTTP interaction across the platform is tagged with a trace identifier (`X-Request-Id`), contextually enriched with user identity, device footprint, and network coordinates, and emitted as high-signal, machine-parseable JSON lines to both console stdout and a dedicated rolling audit log file (`logs/audit.log`).

```
+---------------------------------------------------------------------------------------------------+
|                                        Client Request                                             |
|                             [X-Device-Id: dev-xyz]  [X-Request-Id: req-abc]                      |
+-------------------------------------------------+-------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
|  ActionAuditFilter (Order: HIGHEST_PRECEDENCE)                                                    |
|  - Generates or propagates X-Request-Id                                                           |
|  - Resolves Real Client IP (X-Forwarded-For, CF-Connecting-IP, X-Real-IP)                          |
|  - Injects requestId, clientIp, deviceId, method, uri into SLF4J MDC                              |
|  - Attaches X-Request-Id response header                                                          |
+-------------------------------------------------+-------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
|  JwtAuthenticationFilter                                                                          |
|  - Validates JWT & resolves User identity                                                         |
|  - Enriches SLF4J MDC with userId, userEmail, userRole                                            |
+-------------------------------------------------+-------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
|  Controllers, Services, AI Engines (Execution)                                                    |
|  - All application log statements automatically inherit trace & user context                      |
+-------------------------------------------------+-------------------------------------------------+
                                                  | (finally)
                                                  v
+---------------------------------------------------------------------------------------------------+
|  ActionAuditFilter Completion Callback                                                            |
|  - Measures elapsed latency (durationMs)                                                          |
|  - Extracts HTTP status code                                                                      |
|  - Normalizes URI & maps to semantic domain action (e.g. TASK_GENERATE, GENERATION_TAKEOVER)      |
|  - Emits JSON structured audit record to logger "com.linguaoptima.audit"                          |
|  - Clears MDC context (prevents thread-pool leakage)                                              |
+-------------------------------------------------+-------------------------------------------------+
                                                  |
                        +-------------------------+-------------------------+
                        |                                                   |
                        v                                                   v
         +-----------------------------+                     +-----------------------------+
         |     Console (Stdout)        |                     |      Rolling File           |
         |  Docker Logs / Systemd      |                     |    logs/audit.log           |
         |  Real-time developer stream |                     |  Gzip rotated, 60-day cap   |
         +-----------------------------+                     +-----------------------------+
```

---

## 2. Structured Audit Event Schema

Each line in `logs/audit.log` is a standalone JSON object adhering to the schema below:

```json
{
  "timestamp": "2026-10-10T14:32:15.892Z",
  "type": "ACTION_AUDIT",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "TASK_GENERATE",
  "method": "POST",
  "uri": "/api/tasks/generate",
  "status": 200,
  "durationMs": 1420,
  "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "userEmail": "student@linguaoptima.com",
  "role": "STUDENT",
  "clientIp": "198.51.100.42",
  "deviceId": "dev-9f82k1n8-0b2a",
  "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
}
```

### Schema Field Reference

| Field | Type | Description |
|---|---|---|
| `timestamp` | String (ISO-8601) | Precise UTC timestamp of request completion. |
| `type` | String | Static category identifier (`"ACTION_AUDIT"`). |
| `requestId` | String (UUID / trace) | Distributed correlation ID originating from client or generated server-side. |
| `action` | String (Enum) | Normalized semantic action name. |
| `method` | String | HTTP method (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`). |
| `uri` | String | Target resource URI. |
| `status` | Integer | HTTP response status code (`200`, `201`, `400`, `401`, `403`, `409`, `500`). |
| `durationMs` | Long | Total end-to-end execution latency in milliseconds. |
| `userId` | String (UUID / `"anonymous"`) | Database ID of authenticated actor, or `"anonymous"`. |
| `userEmail` | String | User email address. |
| `role` | String | Security role: `STUDENT`, `TEACHER`, `ADMIN`, or `ANONYMOUS`. |
| `clientIp` | String | Genuine client IPv4/IPv6 address resolved across reverse proxies. |
| `deviceId` | String | Client hardware/browser session token (`X-Device-Id`). |
| `userAgent` | String | Browser or client platform User-Agent string. |

---

## 3. Platform Action Taxonomy

The `ActionAuditFilter` dynamically maps normalized API endpoints into high-level operational domain actions:

### Authentication & Account Lifecycle
* `AUTH_LOGIN` - Credential login attempt.
* `AUTH_REGISTER` - New account registration.
* `AUTH_SEND_CODE` - Email OTP verification dispatch.
* `AUTH_REFRESH` - Silent JWT token refresh exchange.
* `AUTH_FORGOT_PASSWORD` - Password recovery code requested.
* `AUTH_RESET_PASSWORD` - Password reset completed with verification code.
* `AUTH_GOOGLE_OAUTH` - Google OAuth2 authentication.

### AI Task Generation & Concurrency Control
* `TASK_GENERATE` - AI exercise generation invoked (Student or Educator).
* `GENERATION_TAKEOVER` - Session mutex transfer between devices (`/api/tasks/takeover`).
* `TASK_CREATE_TEMPLATE` - Educator saving custom task preset.
* `TASK_ASSIGN_COHORT` - Educator dispatching assignment to student cohorts with deadlines and attempt quotas.

### Submissions & Grading
* `SUBMISSION_TEXT_EVAL` - Student submitting typed text for automated AI rubric evaluation.
* `SUBMISSION_IMAGE_EVAL` - Student submitting photo/scan for OCR extraction + AI evaluation.
* `SUBMISSION_TEACHER_OVERRIDE` - Educator manually overriding AI grade, providing grade adjustment notes.

### Groups & Cohort Collaboration
* `GROUP_CREATE` - Teacher initializing class cohort.
* `GROUP_INVITE_STUDENT` - Student added or invited to cohort.
* `GROUP_INVITATION_ACCEPT` - Student accepting cohort membership.
* `GROUP_INVITATION_DECLINE` - Student declining cohort membership.

### Diagnostics & Reporting
* `REPORT_EXPORT` - Educator generating CSV / PDF cohort analytics.
* `PROGRESS_INSPECT` - Viewing CEFR mastery curves and error breakdown.
* `CUSTOM_CURRICULUM_MUTATE` - Uploading or removing custom syllabus context files.
* `BYOK_KEY_MUTATE` - Storing or deleting custom encrypted vendor API keys.

---

## 4. Logback Configuration & File Rotation

Configured via `backend/src/main/resources/logback-spring.xml`:

* **Log Location:** `logs/audit.log` (configurable via `LOG_PATH` environment variable).
* **Archive Path:** `logs/archived/audit-%d{yyyy-MM-dd}.%i.log.gz`.
* **Rotation Policy:** `SizeAndTimeBasedRollingPolicy`.
* **Max File Size:** `100 MB` per file chunk.
* **Retention:** `60 days` of compressed daily archives.
* **Total Size Cap:** `2 GB` maximum disk budget for audit logs.

---

## 5. Security, Zero-Retention & PII Privacy

1. **Zero Secret Leakage:** Authentication tokens (`Authorization: Bearer ...`), passwords (`password`), and recovery secrets are never written to audit logs.
2. **Zero OCR Retention:** In accordance with the project's Zero-Retention OCR architecture, raw image bytes and binary payloads are never persisted in log sinks.
3. **MDC Sanitization:** SLF4J MDC is strictly cleaned up in a `finally` block on every request to ensure no state leakage across reused Tomcat thread pool workers.

---

## 6. Centralized Ingestion & Querying Guide

### Querying with `jq` (CLI)

```bash
# Tail live audit events
tail -f logs/audit.log | jq .

# Find all concurrent device generation takeovers
cat logs/audit.log | jq 'select(.action == "GENERATION_TAKEOVER")'

# Filter failed requests (status >= 400)
cat logs/audit.log | jq 'select(.status >= 400) | {timestamp, action, status, userEmail, clientIp}'

# Measure average generation latency
cat logs/audit.log | jq -s '[.[] | select(.action == "TASK_GENERATE") | .durationMs] | add / length'
```

### Ingestion into Grafana Loki / Promtail

```yaml
# promtail-config.yml
scrape_configs:
  - job_name: lingua-optima-audit
    static_configs:
      - targets: [localhost]
        labels:
          app: lingua-optima
          log_type: audit
          __path__: /var/log/lingua-optima/audit.log
    pipeline_stages:
      - json:
          expressions:
            timestamp: timestamp
            action: action
            status: status
            userId: userId
            durationMs: durationMs
      - labels:
          action:
          status:
```

### Ingestion into Elasticsearch / Logstash

```ruby
# logstash.conf
input {
  file {
    path => "/var/log/lingua-optima/audit.log"
    codec => "json"
  }
}
filter {
  date {
    match => [ "timestamp", "ISO8601" ]
  }
}
output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "lingua-optima-audit-%{+YYYY.MM.dd}"
  }
}
```
