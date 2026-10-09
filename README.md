# 📚 Lingua Optima — Adaptive AI-Powered English Mastery Platform

> **An interactive platform for mastering the English language (CEFR B1–C1)** featuring AI-driven task generation (Groq Llama 3.3 70B & Llama 4), essay evaluation aligned with IELTS/CEFR rubrics (Google Gemini 2.5 Flash & 3.0 Flash), multi-provider & multi-model BYOK routing (DeepSeek V3.2/R1, Alibaba Qwen 3, Moonshot Kimi K2, OpenAI GPT-5, Anthropic Claude Sonnet 4.6), handwritten homework recognition (Zero-Retention Tesseract OCR), and Computerized Adaptive Testing (CAT).

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
| **AI & OCR Integration** | [`docs/AI_INTEGRATION.md`](./docs/AI_INTEGRATION.md) | Provider & model selection, Cloudflare Edge reverse-proxy, fallback chain (BYOK → Groq → Gemini), prompt leak sanitization, Zero-Retention RAM OCR pipeline, and CAT algorithm |
| **Cloudflare Edge AI Proxy** | [`cloudflare-proxy/README.md`](./cloudflare-proxy/README.md) | Cloudflare Worker (`worker.js` / `ai-proxy.mybsu.online`) reverse-proxy architecture, route mapping for all 7 AI providers, and deployment guide |
| **DevOps & Infrastructure** | [`docs/DEVOPS.md`](./docs/DEVOPS.md) | Docker Compose, multi-stage Dockerfiles, Nginx, Cloudflare Tunnel & Edge Worker topology, GitHub Actions CI/CD, and `.env` configuration |
| **Interactive Presentation** | [`docs/presentation.html`](./docs/presentation.html) | Interactive Pitch Deck and visual architecture walkthrough (opens directly in any browser) |
| **Doxygen Reference (HTML)** | [`docs/generated/html/index.html`](./docs/generated/html/index.html) | Automatically generated Doxygen documentation covering all packages, classes, methods, and types |


---

## 🚀 Core Capabilities & Architectural Principles

1. **6 Core Modules**:
   - **Self-Service Task Generator** — AI generation of exercises (`MCQ`, `GAP_FILL`, `REWRITE`, `ESSAY`) across 36 CEFR grammar topics (B1, B2, C1) with per-task **AI Provider & Model Selection** (DeepSeek V3.2/R1, Qwen 3 235B/QwQ, Kimi K2, Gemini 2.5/3.0, Groq Llama 3.3/4, GPT-5/4.1, Claude Sonnet/Opus 4.6).
   - **Zero-Retention Homework OCR** — handwritten homework photo recognition processed strictly in RAM with immediate byte-array zeroing (`Arrays.fill(bytes, (byte) 0)`).
   - **AI Essay Scoring** — automated essay grading across 4 rubric criteria (*Task Achievement*, *Coherence & Cohesion*, *Lexical Resource*, *Grammatical Range & Accuracy*).
   - **Computerized Adaptive Testing (CAT)** — real-time question difficulty adjustment (scale 1–5) and difficulty-weighted mastery score calculation.
   - **Progress & Gap Analytics** — topic mastery radar charts, weak-spot identification (`masteryScore < 60%`) and automated CEFR level-up recommendations (`>= 80%` across all topics in the current level).
   - **Educator Portal** — student group management (up to 200 students per group), task deployment with AI model customization, manual AI grade adjustments (*Teacher Override*), and **PDF / CSV** report exports.

2. **Security, Authentication & Privacy (Security by Design)**:
   - **Dual Authentication (Email + Google OAuth2)** — supports classic email and password sign-in/registration (`POST /api/auth/login`, `POST /api/auth/register`) as well as one-click sign-in via **Google OAuth2 (Google Identity Services)** (`POST /api/auth/google`) with ID token verification (`email_verified`, `aud`) via the Google `tokeninfo` API and automatic profile provisioning on first login.
   - **In-Memory Access JWT (15 min)** + **HttpOnly Strict Refresh Cookie (30 days)** backed by SHA-256 session token hashes in Redis.
   - **AES-256-GCM Encryption & Multi-Model BYOK** for user-supplied API keys (`provider` + `model_name`, 12-byte random IV + 128-bit authentication tag).
   - **Cloudflare Edge AI Reverse-Proxy & Geo-Unrestricted Routing** (`cloudflare-proxy/worker.js`) — routes Western AI API traffic through Cloudflare Edge (`*_BASE_URL`) to bypass regional GeoIP restrictions while supporting direct low-latency Chinese providers (`DEEPSEEK`, `QWEN`, `KIMI`).
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
