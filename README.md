# Lingua Optima — Adaptive AI-Powered English Mastery Platform

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker_Compose-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![JaCoCo Coverage](https://img.shields.io/badge/JaCoCo-99.7%25_Lines_%7C_99.3%25_Instr-brightgreen?style=for-the-badge&logo=codecov&logoColor=white)](./docs/BACKEND.md)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](./LICENSE)

**An enterprise-grade, interactive learning and assessment platform for mastering the English language across the entire CEFR continuum (A1–C2).**  
Featuring AI-driven task generation, essay grading aligned with IELTS/CEFR rubrics, multi-provider BYOK model routing with Cloudflare Edge proxying, Computerized Adaptive Testing (CAT), handwritten homework recognition via Zero-Retention Tesseract OCR, educator cohort orchestration with cross-group assignment transfer, and a structured enterprise action audit logging subsystem with distributed request correlation tracing.

</div>

---

## Interactive Presentation and Documentation

> **[Interactive Project Presentation (`docs/presentation.html`)](./docs/presentation.html)** — A dynamic slide presentation and architecture deck runnable in any browser.  
> **[Compiled Doxygen Reference (`docs/generated/html/index.html`)](./docs/generated/html/index.html)** — Full code documentation generated from Javadoc and TypeScript docstrings.

### Documentation Index

| Document | Link | Core Focus |
|---|---|---|
| **Master Architecture** | [`docs/README.md`](./docs/README.md) | Full system specification, database ERD, complete REST API table, and security design |
| **Action Audit & Logging** | [`docs/AUDIT_LOGGING.md`](./docs/AUDIT_LOGGING.md) | Structured JSON action audit logging, SLF4J MDC request tracing, and security events |
| **Backend Specification** | [`docs/BACKEND.md`](./docs/BACKEND.md) | Java 21 / Spring Boot 3 architecture, all controllers, services, repositories, and migrations |
| **Frontend Specification** | [`docs/FRONTEND.md`](./docs/FRONTEND.md) | React 18 + TypeScript + Vite PWA architecture, components, Zustand stores, and routing |
| **AI & OCR Integration** | [`docs/AI_INTEGRATION.md`](./docs/AI_INTEGRATION.md) | Model selection, live sync, Cloudflare Edge proxy, RAM OCR pipeline, and CAT algorithm |
| **Cloudflare Edge AI Proxy** | [`cloudflare-proxy/README.md`](./cloudflare-proxy/README.md) | Cloudflare Worker (`worker.js`) reverse-proxy, live vendor model scraper, and routing |
| **DevOps & Infrastructure** | [`docs/DEVOPS.md`](./docs/DEVOPS.md) | Docker Compose, Nginx, Cloudflare Tunnel, GitHub Actions CI/CD, and `.env` guide |
| **Interactive Presentation** | [`docs/presentation.html`](./docs/presentation.html) | Interactive pitch deck, architecture flowcharts, and system walkthrough |
| **Doxygen Reference (HTML)** | [`docs/generated/html/index.html`](./docs/generated/html/index.html) | Auto-generated Doxygen API reference covering all backend and frontend classes |

---

## High-Level System Architecture

```
                          [ Web Browser / Mobile PWA ]
                                       │
                    HTTPS / TLS (X-Request-Id, X-Device-Id)
                                       ▼
                       ┌───────────────────────────────┐
                       │       Nginx Reverse Proxy     │
                       └───────────────┬───────────────┘
                                       │
                  ┌────────────────────┴────────────────────┐
                  ▼                                         ▼
   ┌─────────────────────────────┐           ┌─────────────────────────────┐
   │    Frontend SPA (Vite)      │           │ Spring Boot API (Java 21)   │
   │  React 18 + TypeScript      │           │ ActionAuditFilter (MDC)     │
   │  Zustand, Tailwind, KaTeX   │           │ JwtAuthenticationFilter     │
   └─────────────────────────────┘           └──────────────┬──────────────┘
                                                            │
               ┌───────────────────────┬────────────────────┼───────────────────────┐
               ▼                       ▼                    ▼                       ▼
      ┌─────────────────┐     ┌─────────────────┐  ┌─────────────────┐     ┌─────────────────┐
      │  PostgreSQL 16  │     │     Redis 7     │  │ Zero-Retention  │     │ Cloudflare Edge │
      │  Relational DB  │     │ Sessions, Cache │  │  Tesseract OCR  │     │   AI Proxy &    │
      │  Flyway Schema  │     │ Rate Limiting   │  │ (RAM processing)│     │  BYOK Routing   │
      └─────────────────┘     └─────────────────┘  └─────────────────┘     └────────┬────────┘
                                                                                    │
                                           ┌────────────────────────────────────────┴────────┐
                                           ▼                                                 ▼
                              [ Western AI Providers ]                              [ Chinese Providers ]
                              Gemini 3.8 Flash / Thinking                           DeepSeek V4.1 / Pro
                              Groq Qwen 27B / Llama 4                               Alibaba Qwen 3.8 Max
                              OpenAI GPT-6 / Claude 5.1                             Moonshot Kimi K3
```

---

## Core Capabilities and Architecture

### 1. Computerized Adaptive Testing (CAT) & Item Response Theory
- Dynamic difficulty adjustment (scale 1–4) responding in real-time to student correctness.
- Difficulty-weighted scoring: questions at higher difficulty contribute proportionally greater weight to the final score.
- Rich question-level evaluation records containing exact question sentences, grammar rule diagnostics, and option breakdowns.

### 2. Resilient Mastery Engine & Automated CEFR Progression
- **6-Level CEFR Progression Ladder**: Complete calibration across **A1 Beginner**, **A2 Elementary**, **B1 Intermediate**, **B2 Upper-Intermediate**, **C1 Advanced**, and **C2 Mastery**.
- **Topic Keyword Normalization**: The mastery calculation engine (`ProgressService.normalizeTopicKey`) uses sorted keyword stemming and stop-word filtering to reliably match curriculum syllabus topics across subtle orthographic variations (e.g. `Third & Mixed Conditionals` vs `Third Conditional & Mixed Conditionals`).
- **Strict Mutual Exclusivity**: Diagnostic strengths (`masteryScore >= 85%`) and weaknesses are guaranteed disjoint—mastered rules are automatically excluded from deficiency lists and recommendations.
- **Automated Level-Up Suggestion**: Achieved when a student demonstrates `>= 85%` mastery across `>= 80%` of syllabus topics for their current level.

### 3. Educator Cohort Management & Multi-Group Deployment
- **Multi-Cohort Task Generation (`POST /api/tasks/generate-multi`)**: Allows teachers to create a task once and distribute it simultaneously to multiple student groups with unique cohort-scoped assignments.
- **Cohort-to-Cohort Task Transfer (`POST /api/tasks/{id}/transfer`)**: One-click transfer or copy of assigned tasks from one cohort to another, preserving point budgets and question keys.
- **Grading & Feedback Editor**: Teachers can inspect itemized student submissions, provide granular feedback, and override AI-assigned scores without losing AI diagnostics.
- **Intra-Group Leaderboard**: Student performance is ranked privately within their assigned cohort using anonymized aliases (`displayAlias`), upholding student privacy.
- **Academic Report Exports**: Comprehensive PDF and CSV exports detailing homework completion statuses, attempt counters, point breakdowns, and teacher remarks.

### 4. Zero-Retention RAM-Only Homework OCR
- **Privacy by Design**: Student photos of handwritten homework are decoded directly in volatile memory (RAM).
- **Immediate Byte Zeroing**: Image byte buffers are scrubbed immediately after text extraction (`Arrays.fill(bytes, (byte) 0)`). No image binaries or biometric photos are ever written to disk or the database.
- Multi-photo attachment support (up to 5 images, aggregate 25 MB limit).

### 5. Multi-Provider BYOK & Cloudflare Edge AI Proxy
- **Unified Routing**: Out-of-the-box support for 7 leading AI providers with live model catalog synchronization (`https://ai-proxy.mybsu.online`).
- **AES-256-GCM BYOK Encryption**: User-supplied API keys are encrypted at rest with random 12-byte initialization vectors and 128-bit authentication tags.
- **Automatic Fallback Chain**: System-level fallback gracefully reroutes failed inference requests to secondary providers before queuing.

### 6. Reading Comprehension Studio, Window Scaling & Diagnostic Error Annotation
- **Reading Comprehension Engine**: Generates coherent thematic reading texts with calibrated CEFR vocabulary, customizable passage length (Short 150–250w, Medium 250–400w, Long 400–600w, or Custom numeric word counts 50–1000w), and paragraph structural narrative guidelines.
- **Question Modes & Random Surprise Mix**: Enables educators and students to configure specific question counts (MCQ, True/False, Sentence Completion) or activate "Surprise Me (Random Mix)" mode with 1-click re-roll shuffle for diverse exercise formats.
- **Interactive Reading Window Scaling & Typography Controls**: Responsive two-column reading viewport with column split width presets (Compact 35%, Balanced 50%, Wide 58%, Expanded 67% with auto-expanding widescreen container up to 7XL), dynamic typography resizing (A-/A+ with 4 font sizes sm/base/lg/xl), and full-height scroll toggle.
- **Mixed Question Architectures & Point Allocation**: Supports multi-category reading tasks combining Multiple Choice (`MCQ`), True / False / Not Given (`True / False`), and Sentence Completion / Open-Ended questions with customizable point weighting per question category.
- **Semantic AI Evaluation of Open Questions**: Intelligent semantic evaluation ensures alternative, accurate student phrasings and formulations receive full credit rather than being penalized for non-verbatim matches with the reference answer key.
- **Inline Grammatical Mistake Highlighting & Educator Review**: Highlights grammatical and stylistic errors directly within student essays and open responses in red with contextual popovers detailing the error, suggested rewrite, grammar rule, and explanation. Educators can dynamically highlight new mistakes, edit explanations, toggle question correctness, and remove highlights.

### 7. Speaking & Pronunciation Practice Studio ($0-Budget Acoustic Architecture)
- **AI Speech Generation**: Generation of oral reading passages calibrated to CEFR levels (A1–C2) with targeted phonetic patterns (dental fricatives `/θ/-/ð/`, liquids `/r/-/l/`, labiodentals `/v/-/w/`, vowel contrasts `/iː/-/ɪ/`, sibilants, silent letters, connected speech rhythm) and duration presets.
- **Light Theme & Custom Scenario Calibration**: Modern Lingua Optima light gradient styling (`indigo-50/40 via-white to violet-50/30`), clear educational phonetic descriptions with everyday examples, and custom speech length exposing numeric target word count (15–300 words) and custom speech scenario / monologue instructions.
- **Client-Side Phonetic Alignment & Zero-Cost Scoring**: Browser-native Web Speech API (`SpeechRecognition`) captures microphone audio without recurring cloud API fees; client-side dynamic Levenshtein alignment categorizes spoken words as correct, mispronounced, or omitted.
- **Speech Rate & Pacing Cadence Scoring**: Real-time measurement of reading speed in Words Per Minute (WPM) calibrated against natural conversational tempo (100–160 WPM). Pacing directly influences fluency and composite overall scoring alongside pronunciation accuracy and completeness.
- **IPA Transcription & Multimodal Articulation Guidance**: Integrated phonetics dictionary with suffix heuristics provides full IPA transcriptions and clickable articulatory placement guides for mispronounced tokens.
- **Educator Studio & Student Self-Service**: Teachers can customize, preview, listen to text-to-speech synthesized audio, edit reading texts, and review student audio attempts with accuracy diagnostics and WPM speed indicators. Students can autonomously configure target phonemes and generate custom drills.
- **Enterprise Upgrade Readiness**: Cleanly decoupled engine contracts enable swapping client-side speech recognition with cloud acoustic models (e.g. Azure Pronunciation Assessment, Whisper) without database or UI refactoring.

### 8. JSON-Calibrated Pronunciation Packs & Speaking Eco Mode
- **Structured Phonetics Resource**: 36 calibrated pronunciation packs stored in JSON resource (`backend/src/main/resources/phonetics/pronunciation_packs.json`) across all CEFR tiers (A1 Beginner to C2 Mastery). Covers dental fricatives (`/θ/`, `/ð/`), liquids (`/r/`, `/l/`), labiodentals (`/v/`, `/w/`), vowel duration contrasts (`/iː/` vs `/ɪ/`, `/uː/` vs `/ʊ/`), sibilants, silent consonants, and connected speech assimilation.
- **Minimal Pair Contrast Modeling**: Each pack includes target drill words, focus sound descriptions, and minimal pair contrast matrices (e.g., *ship* vs *sheep*, *think* vs *sink*, *berry* vs *very*) for targeted auditory and articulatory distinction.
- **Speaking Eco Mode**:
  - **Eco Mode OFF**: Prompts are dynamically enriched with JSON target sound lists and minimal pair contrasts, instructing the neural network to weave high-density phonetic drills into the passage.
  - **Eco Mode ON**: Streamlines prompt structure, bypassing heavy phonetic lists to reduce token consumption by up to 45% and minimize generation latency.

### 9. Zero-Duplication Modular Task Studios
- **Syllabus Isolation**: Replaces disjointed configuration inputs with dedicated, cohesive panels: `GrammarConfigPanel`, `ReadingConfigPanel`, and `SpeakingConfigPanel`.
- **Contextual Clarity**: Topic selectors, grammar rules, and target vocabulary appear exclusively for grammar exercises; reading passages expose only text length, paragraphs, and question distributions; speaking configurations focus purely on acoustic sounds, duration, and pronunciation packs.
- **Unified Controls**: AI engine selection, cohort assignment, and CEFR level are maintained as clean top-level controls with zero field duplication.

### 10. Quick Practice Mode (Быстрый режим)
- **1-Click Generation**: Streamlined mode requiring only CEFR level, practice format (Surprise Mix, Grammar Quiz, Reading Article, Speaking Drill), and topic.
- **Surprise Topic Shuffle**: Instant randomized topic shuffle (`🎲`) across the CEFR curriculum catalog or direct custom topic entry.
- **Automatic Parameter Balancing**: Difficulty tier, question counts, narrative paragraphs, and real-world domain contexts are automatically balanced behind the scenes with Eco Mode enabled by default.

### 11. Cross-Interface Curriculum Persistence
- **Teacher-to-Student Synchronization**: Custom grammar rules and vocabulary sets saved by educators are automatically inherited by their enrolled students via cohort group relationships.
- **Unified Library**: Students see both their personal custom rules and their teachers' verified curriculum entries clearly labeled with teacher attribution in task generation and homework execution interfaces.

### 12. The 4 Pedagogical Task Pillars
Lingua Optima structures all learning and assessment around four foundational pillars:
- **Grammar & Mechanics**: Multiple choice, gap fills, bracket transformations, and sentence rewrites with instant feedback and exact rule attribution.
- **Reading Comprehension**: Rich thematic passages with calibrated word counts, responsive split viewports, and semantic AI grading of open-ended answers.
- **Speaking & Pronunciation**: Browser-native Web Speech API phonetic alignment, 36 CEFR pronunciation packs, minimal pairs, and cadence/WPM diagnostics.
- **Writing & Essay**: Authentic IELTS/Cambridge 4-criteria rubric evaluation (Task Achievement, Coherence & Cohesion, Lexical Resource, Grammatical Range) with autosaved drafts.

### 13. Card Focus Mode & Hotkey Navigation
- **Distraction-Free Step-by-Step Solving**: Switch seamlessly between Classic List, Card Focus Mode, and Presentation Mode in `TaskView.tsx`.
- **Keyboard Hotkeys**: Select options instantly with keys `1`–`4`, navigate cards with `ArrowLeft` and `ArrowRight`, and advance via `Enter`.
- **Instant Feedback (`Мгновенная проверка`)**: Optional self-practice mode immediately validates selections with emerald/rose borders, checkmark/cross status icons, and underlying grammar rule insights.

### 14. Sentence AI Explainer Engine (`POST /api/tasks/explain-sentence`)
- **Real-Time Contextual Breakdown**: Highlight text or click `💡 Пояснить` on any reading sentence or exercise item to open an AI analysis drawer.
- **Granular Linguistic Insights**: Delivers natural Russian translation, syntactic tense/pattern breakdown, and key vocabulary items without robotic AI filler.

### 15. Classroom Live & Presentation Mode
- **Smartboard & Projector Ready**: Fullscreen viewport with high-contrast, scalable typography (`text-2xl` to `text-3xl`).
- **Interactive Answer Revelation**: Educators can toggle `[ 👁️ Показать правильный ответ ]` / `[ 🙈 Скрыть ответ ]` per question during lectures or group reviews.

### 16. Smart Recommendations, Confetti & Mastery Integrity
- **1-Click Gap Targeting**: "Recommended" practice mode automatically queries student mastery records (`/progress/gaps`) and directs learners toward their weakest CEFR grammar gaps.
- **Non-Inflating Score Overrides**: Teacher grading overrides adjust error counters via mathematical deltas (`adjustFromOverride`) rather than creating duplicate attempt records.
- **Celebration Confetti**: Zero-dependency HTML5 Canvas confetti engine triggers bursts on $\ge 90\%$ achievement scores.

---

## Enterprise Action Audit and Distributed Tracing

Lingua Optima features a dedicated enterprise-grade **Action Audit Logging** subsystem designed for security observability, academic integrity compliance, and distributed request tracing.

### Architectural Flow

1. **Request Inception (`ActionAuditFilter`)**:
   - Registered at `Ordered.HIGHEST_PRECEDENCE` ahead of security filters.
   - Extracts or generates a distributed correlation identifier (`X-Request-Id`).
   - Resolves the genuine client IP across multi-tier reverse proxies (`X-Forwarded-For`, `CF-Connecting-IP`, `X-Real-IP`).
   - Populates SLF4J **Mapped Diagnostic Context (MDC)** with `requestId`, `clientIp`, `deviceId`, `method`, and `uri`.
   - Propagates `X-Request-Id` in the HTTP response headers for end-to-end client-server correlation.

2. **Security & Actor Identification (`JwtAuthenticationFilter`)**:
   - Downstream authentication resolves JWT access tokens and authenticates user principals.
   - Context is injected into SLF4J MDC (`userId`, `userEmail`, `role`).

3. **Domain Action Taxonomy Classification**:
   - Request URIs are normalized by stripping UUIDs and numeric IDs.
   - Mapped to standardized semantic domain actions:
     - `AUTH_LOGIN`, `AUTH_REGISTER`, `AUTH_SEND_CODE`, `AUTH_GOOGLE_OAUTH`
     - `TASK_GENERATE`, `TASK_GENERATE_MULTI`, `TASK_TRANSFER_COHORT`, `TASK_ASSIGN_COHORT`
     - `SUBMISSION_TEXT_EVAL`, `SUBMISSION_IMAGE_EVAL`, `SUBMISSION_TEACHER_OVERRIDE`
     - `CAT_SESSION_START`, `CAT_SESSION_ANSWER`, `CAT_SESSION_COMPLETE`
     - `USER_PROFILE_UPDATE`, `USER_GDPR_ERASURE`, `STUDENT_ALIAS_RENAME`
     - `GROUP_CREATE`, `GROUP_INVITE_STUDENT`, `REPORT_EXPORT`

4. **Structured JSON Audit Stream**:
   - Emits a standalone JSON object for every completed request to `logs/audit.log` (logger `com.linguaoptima.audit`).
   - Asynchronous GZIP rotation: 100 MB max file size, 60-day history retention, 2 GB total cap.

### Audit Record Schema Example

```json
{
  "timestamp": "2026-10-15T10:32:15.892Z",
  "type": "ACTION_AUDIT",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "TASK_GENERATE_MULTI",
  "method": "POST",
  "uri": "/api/tasks/generate-multi",
  "status": 200,
  "durationMs": 1845,
  "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "userEmail": "educator@linguaoptima.com",
  "role": "TEACHER",
  "clientIp": "198.51.100.42",
  "deviceId": "dev-9f82k1n8-0b2a",
  "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/130.0.0.0"
}
```

### Logback Appender Hierarchy

```
+-------------------------------------------------------------------------------+
|                            Spring Boot Logging Core                           |
+---------------------------------------┬---------------------------------------+
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        ▼                               ▼                               ▼
 ┌───────────────┐              ┌───────────────┐              ┌───────────────┐
 │ Console (TTY) │              │  App Rolling  │              │ Audit Rolling │
 │   (Stdout)    │              │ lingua-optima │              │   audit.log   │
 │ Colorized MDC │              │ 50MB, 30 days │              │ JSON, 60 days │
 └───────────────┘              └───────────────┘              └───────────────┘
```

---

## Quick Start and Development

### 1. Run with Docker Compose
```bash
docker compose up --build -d
```
- **Frontend SPA**: `http://localhost:5173`
- **Backend REST API**: `http://localhost:8080/api`
- **Actuator Health**: `http://localhost:8080/actuator/health`
- **Adminer DB Interface**: `http://localhost:8081`

### 2. Local Backend Testing & JaCoCo Verification
```bash
cd backend
# Runs 401 unit and slice tests, verifies >=99% instructions and >=99% line coverage
./gradlew test jacocoTestCoverageVerification
```

### 3. Local Frontend Testing & Production Build
```bash
cd frontend
# Runs 58 Vitest unit tests and builds optimized production bundles
npm install
npm test
npm run build
```

### 4. Interactive Documentation Generation (Doxygen & Javadoc)
```bash
# Generate full project documentation (Backend + Frontend + Markdown)
doxygen Doxyfile

# Generate Java API documentation
cd backend && ./gradlew javadoc
```
Output documentation is compiled to `docs/generated/html/index.html`.

---

## Environment Variables (`.env`)

The system loads environment settings from `.env` in the repository root:

| Variable | Description | Default / Example |
|---|---|---|
| `SERVER_PORT` | Spring Boot HTTP server port | `8080` |
| `POSTGRES_DB` | PostgreSQL database name | `lingua_optima` |
| `POSTGRES_USER` | PostgreSQL database user | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL database password | `secret` |
| `SPRING_DATASOURCE_URL` | JDBC database connection URL | `jdbc:postgresql://db:5432/lingua_optima` |
| `REDIS_HOST` | Redis cache and session host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 JWTs | *Base64 string* |
| `ENCRYPTION_KEY` | 256-bit secret key for AES-256-GCM BYOK storage | *Base64 string* |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID for authentication | *Google Cloud Console ID* |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | *Google Cloud Console Secret* |
| `SMTP_HOST` | Gmail SMTP server for 6-digit confirmation codes | `smtp.gmail.com` |
| `SMTP_PORT` | SMTP port | `587` |
| `SMTP_USER` | Gmail address for sending verification emails | `mailer@linguaoptima.com` |
| `SMTP_PASS` | Gmail App Password (16 characters) | `abcd efgh ijkl mnop` |
| `GROQ_API_KEY` | System fallback Groq API key | *Groq Console Key* |
| `GEMINI_API_KEY` | System fallback Google Gemini API key | *Google AI Studio Key* |

---

## Repository Structure

```
lingua_optima/
├── .github/workflows/ci.yml       # Automated GitHub Actions CI pipeline
├── cloudflare-proxy/              # Cloudflare Edge Worker AI proxy & scraper
│   ├── worker.js                  # Edge routing & vendor model synchronization
│   └── README.md                  # Proxy deployment guide
├── backend/                       # Spring Boot 3 + Java 21 REST API
│   ├── src/main/java/             # Controllers, Services, Repositories, Domain, Config
│   ├── src/main/resources/        # application.yml, logback-spring.xml, Flyway migrations
│   └── src/test/java/             # 401 unit & slice tests, 99.7% lines | 99.3% instructions
├── frontend/                      # React 18 + TypeScript + Vite PWA
│   ├── src/components/            # Student, Teacher, Auth, and Common UI components
│   ├── src/pages/                 # Landing, Dashboards, Privacy Policy, Terms, Help Center
│   ├── src/store/                 # Zustand state stores (auth, task, ui, notification)
│   └── src/test/                  # 58 Vitest unit and component tests
├── docs/                          # In-depth architectural specifications
│   ├── presentation.html          # Interactive Project Presentation (Pitch Deck)
│   ├── AUDIT_LOGGING.md           # Structured Action Audit & Distributed Tracing
│   ├── BACKEND.md                 # Backend engineering specification
│   ├── FRONTEND.md                # Frontend engineering specification
│   ├── AI_INTEGRATION.md          # Multi-model routing & Zero-Retention OCR
│   ├── DEVOPS.md                  # Infrastructure, Docker, Nginx, and deployment
│   └── README.md                  # Master architecture and API reference table
├── docker-compose.yml             # Full-stack container orchestration
├── Doxyfile                       # Doxygen API documentation generator config
└── README.md                      # Platform overview and quickstart guide
```

---

## License and Copyright

Copyright (c) 2026 **Maksim Manastyrski**. All rights reserved.  
Licensed under the **[MIT License](./LICENSE)**. See the [`LICENSE`](./LICENSE) file for complete terms and permissions.
