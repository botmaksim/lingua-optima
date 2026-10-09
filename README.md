# 📚 Lingua Optima — Adaptive AI-Powered English Mastery Platform

> **An interactive platform for mastering the English language (CEFR A1–C2)** featuring AI-driven task generation (Groq Qwen 3.8 27B, GPT-OSS 120B & Llama 4), essay evaluation aligned with IELTS/CEFR rubrics (Google Gemini 3.8 Flash & Extended Thinking), multi-provider & multi-model BYOK routing with live vendor model synchronization (DeepSeek V4.1 Flash / V4 Pro, Alibaba Qwen 3.8 Max, Moonshot Kimi K3, OpenAI GPT-6 Astra / 6.1 Sol, Anthropic Claude Fable 5.1 / Opus 5.5 / Sonnet 5.5), handwritten homework recognition (Zero-Retention Tesseract OCR), and Computerized Adaptive Testing (CAT).

---

## 🎬 Interactive Presentation & Documentation

> 📊 **[Open the Interactive Project Presentation (`docs/presentation.html`)](./docs/presentation.html)**  
> 📘 **[Open the Compiled Doxygen Documentation (`docs/generated/html/index.html`)](./docs/generated/html/index.html)**

### 🗂 Complete Documentation Navigator

| Document | Link | Description |
|---|---|---|
| **Master Architecture Overview** | [`docs/README.md`](./docs/README.md) | Full system specification, database ERD, complete REST API reference table, security architecture, and Redis key spaces |
| **Backend Specification** | [`docs/BACKEND.md`](./docs/BACKEND.md) | Java 21 / Spring Boot 3 architecture, all controllers, services, repositories, DTOs, database migrations, and error handling |
| **Frontend Specification** | [`docs/FRONTEND.md`](./docs/FRONTEND.md) | React 18 + TypeScript + Vite PWA architecture, components, Zustand stores, custom hooks, and routing |
| **AI & OCR Integration** | [`docs/AI_INTEGRATION.md`](./docs/AI_INTEGRATION.md) | Provider & model selection, live vendor model sync (`/models/:provider`), Cloudflare Edge reverse-proxy, fallback chain, prompt leak sanitization, Zero-Retention RAM OCR pipeline, and CAT algorithm |
| **Cloudflare Edge AI Proxy** | [`cloudflare-proxy/README.md`](./cloudflare-proxy/README.md) | Cloudflare Worker (`worker.js` / `ai-proxy.mybsu.online`) reverse-proxy architecture, live vendor docs model scraper, route mapping for all 7 AI providers, and deployment guide |
| **DevOps & Infrastructure** | [`docs/DEVOPS.md`](./docs/DEVOPS.md) | Docker Compose, multi-stage Dockerfiles, Nginx, Cloudflare Tunnel & Edge Worker topology, GitHub Actions CI/CD, and `.env` configuration |
| **Interactive Presentation** | [`docs/presentation.html`](./docs/presentation.html) | Interactive Pitch Deck and visual architecture walkthrough (opens directly in any browser) |
| **Doxygen Reference (HTML)** | [`docs/generated/html/index.html`](./docs/generated/html/index.html) | Automatically generated Doxygen documentation covering all packages, classes, methods, and types |


---

## 🚀 Core Capabilities & Architectural Principles

1. **6 Core Modules**:
   - **Self-Service Task Generator** — AI generation of exercises (`MCQ`, `GAP_FILL`, `REWRITE`, `ESSAY`) across 38+ CEFR grammar topics spanning the complete 6-level ladder (**A1 Beginner**, **A2 Elementary**, **B1 Intermediate**, **B2 Upper-Intermediate**, **C1 Advanced**, **C2 Mastery**) with per-task **AI Provider & Model Selection** synced live from vendor websites (DeepSeek V4.1 Flash / V4 Pro, Qwen 3.8 Max / Flash, Kimi K3 / K2.7, Gemini 3.8 Flash / Extended Thinking, Groq Qwen 3.8 27B / GPT-OSS 120B / Llama 4, GPT-6 Astra / 6.1 Sol / Luna, Claude Fable 5.1 / Opus 5.5 / Sonnet 5.5).
   - **Zero-Retention Homework OCR** — handwritten homework photo recognition processed strictly in RAM with immediate byte-array zeroing (`Arrays.fill(bytes, (byte) 0)`).
   - **AI Essay Scoring** — automated essay grading across 4 rubric criteria (*Task Achievement*, *Coherence & Cohesion*, *Lexical Resource*, *Grammatical Range & Accuracy*).
   - **Computerized Adaptive Testing (CAT)** — real-time question difficulty adjustment (scale 1–5) and difficulty-weighted mastery score calculation.
   - **Progress & Gap Analytics** — topic mastery radar charts, weak-spot identification (`masteryScore < 60%`), baseline start at A1, and automated CEFR level-up recommendations (`>= 85%` mastery across `>= 80%` of syllabus topics).
   - **Educator Portal & Cohort Management** — student group/cohort management (up to 200 students per group, join codes, email invitations), task deployment with AI model customization, manual AI grade adjustments (*Teacher Override*), and **PDF / CSV** report exports.
   - **Self-Service Role Switching** — instant switching between Student and Educator modes via Profile (`PUT /api/users/me`), allowing users to both learn and teach.

2. **Security, Authentication & Privacy (Security by Design)**:
   - **Dual Authentication (Email + Google OAuth2) with 2-Step Email Verification** — supports classic email and password registration with mandatory 2-step verification (`POST /api/auth/send-verification-code` dispatches a 6-digit confirmation code via Gmail SMTP with a 60-second rate-limiting cooldown and 10-minute expiry; `POST /api/auth/register` verifies the code) as well as seamless one-click sign-in via **Google OAuth2 (Google Identity Services)** (`POST /api/auth/google`) with automatic profile provisioning on first login.
   - **In-Memory Access JWT (15 min)** + **HttpOnly Strict Refresh Cookie (30 days)** backed by SHA-256 session token hashes in Redis.
   - **AES-256-GCM Encryption & Multi-Model BYOK** for user-supplied API keys (`provider` + `model_name`, 12-byte random IV + 128-bit authentication tag).
   - **Cloudflare Edge AI Reverse-Proxy & Live Vendor Model Sync** (`cloudflare-proxy/worker.js` at `https://ai-proxy.mybsu.online`) — routes Western AI API traffic through Cloudflare Edge (`*_BASE_URL`) to bypass regional GeoIP restrictions, supports direct low-latency Chinese providers (`DEEPSEEK`, `QWEN`, `KIMI`), and dynamically scrapes official vendor documentation (`/models/:provider`) so the UI always displays the latest models.
   - **Strict AI Prompt Sanitization** (`TaskService`, `ScoringService`, `textSanitizer.ts`) — prevents internal system instructions or rubric prompts from ever leaking into student-facing task content or feedback.
   - **Private Group Leaderboard** — no global public leaderboard; rankings are computed strictly within a teacher's student group using anonymized aliases (`displayAlias`).
   - **Soft Delete in Groups** (`is_active = false`, `removed_at`) — when a student is removed from a group, their past submissions are hidden from the teacher, and if re-added later, their historical submissions are seamlessly restored.
   - **GDPR Right to Erasure** (`DELETE /api/users/me`) — complete erasure of PII and anonymization of historical submissions.

---

## ⚙️ Environment Variables Configuration (`.env`)

The repository root contains the template [`.env.example`](./.env.example) and the active `.env` file (excluded via `.gitignore`). All variables are automatically loaded by the **Backend** (`spring.config.import` in `application.yml`), the **Frontend** (`envDir: '..'` in `vite.config.ts`), and **Docker Compose**:

- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `REDIS_HOST`, `REDIS_PORT`
- `SERVER_PORT`, `CORS_ORIGINS`, `VITE_API_URL`
- `JWT_SECRET`, `ENCRYPTION_KEY`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `VITE_GOOGLE_CLIENT_ID` *(to enable Google OAuth2 login, set your OAuth 2.0 Client ID from the Google Cloud Console in `GOOGLE_CLIENT_ID` and `VITE_GOOGLE_CLIENT_ID`, and the secret in `GOOGLE_CLIENT_SECRET`)*
- `GROQ_API_KEY`, `GROQ_BASE_URL`, `GEMINI_API_KEY`, `GEMINI_BASE_URL`
- `OPENAI_BASE_URL`, `ANTHROPIC_BASE_URL`
- `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL`, `QWEN_API_KEY`, `QWEN_BASE_URL`, `KIMI_API_KEY`, `KIMI_BASE_URL`
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS` *(Gmail SMTP credentials for dispatching 6-digit registration confirmation emails)*
- `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`


---

## 🛠 Quick Start & Build

### 1. Run via Docker Compose
```bash
docker compose up --build -d
```
- **Frontend (Nginx PWA)**: `http://localhost:5173`
- **Backend API (Spring Boot)**: `http://localhost:8080/api`
- **Adminer (DB UI)**: `http://localhost:8081`

### 2. Local Development & Testing
```bash
# Backend: run 222 unit tests and verify JaCoCo coverage (100.00% instructions, 100.00% branches, 100.00% lines, 100.00% methods, 100.00% classes)
cd backend && ./gradlew test jacocoTestCoverageVerification

# Frontend: run Vitest unit tests and production build
cd frontend && npm install && npm test && npm run build
```

### 3. Documentation Compilation (Doxygen & Javadoc)
```bash
# Generate full HTML documentation (Backend + Frontend + Markdown + Presentation)
doxygen Doxyfile

# Generate Javadoc for Backend
cd backend && ./gradlew javadoc
```
Compiled documentation is saved to `docs/generated/html/index.html` (the `docs/generated/` directory is listed in `.gitignore`).
