# 📚 Lingua Optima — Adaptive AI-Powered English Mastery Platform

> **Интерактивная платформа для изучения английского языка (CEFR B1–C1)** с генерацией заданий через ИИ (Groq Llama 3.1 70B), проверкой эссе по рубрике IELTS/CEFR (Google Gemini 1.5 Flash), распознаванием рукописных домашних работ (Zero-Retention Tesseract OCR) и компьютерным адаптивным тестированием (CAT).

---

## 🎬 Интерактивная презентация и Документация

> 📊 **[Открыть интерактивную презентацию проекта (`docs/presentation.html`)](./docs/presentation.html)**  
> 📘 **[Открыть скомпилированную Doxygen документацию (`docs/generated/html/index.html`)](./docs/generated/html/index.html)**

### 🗂 Полный навигатор по документации проекта

| Документ | Ссылка | Описание |
|---|---|---|
| **Главный архитектурный обзор** | [`docs/README.md`](./docs/README.md) | Полная спецификация системы, ERD базы данных, сводная таблица REST API, безопасность, Redis |
| **Backend Specification** | [`docs/BACKEND.md`](./docs/BACKEND.md) | Архитектура Java 21 / Spring Boot 3, все контроллеры, сервисы, репозитории, DTO, миграции и обработка ошибок |
| **Frontend Specification** | [`docs/FRONTEND.md`](./docs/FRONTEND.md) | Архитектура React 18 + TypeScript + Vite PWA, компоненты, Zustand-сторы, кастомные хуки, маршрутизация |
| **AI & OCR Integration** | [`docs/AI_INTEGRATION.md`](./docs/AI_INTEGRATION.md) | Цепочка провайдеров (BYOK → Groq → Gemini), промпт-инжиниринг, Zero-Retention OCR в RAM, алгоритм CAT |
| **DevOps & Infrastructure** | [`docs/DEVOPS.md`](./docs/DEVOPS.md) | Docker Compose, мультистейдж Dockerfiles, Nginx, GitHub Actions CI/CD, конфигурация `.env` |
| **Интерактивная презентация** | [`docs/presentation.html`](./docs/presentation.html) | Интерактивный Pitch Deck и визуализация архитектуры платформы (открывается в любом браузере) |
| **Doxygen Reference (HTML)** | [`docs/generated/html/index.html`](./docs/generated/html/index.html) | Автоматически сгенерированная документация Doxygen по всем пакетам, классам, методам и типам |


---

## 🚀 Ключевые возможности и архитектурные принципы

1. **6 ключевых модулей**:
   - **Self-Service Task Generator** — генерация упражнений (`MCQ`, `GAP_FILL`, `REWRITE`, `ESSAY`) по 36 грамматическим темам CEFR (B1, B2, C1).
   - **Zero-Retention Homework OCR** — распознавание фото рукописных работ строго в оперативной памяти (RAM) с немедленным занулением байтового массива (`Arrays.fill(bytes, (byte) 0)`).
   - **AI Essay Scoring** — оценка эссе по 4 критериям (*Task Achievement*, *Coherence & Cohesion*, *Lexical Resource*, *Grammatical Range & Accuracy*).
   - **Computerized Adaptive Testing (CAT)** — динамическая подстройка сложности вопросов (шкала 1–5) в реальном времени и расчёт mastery score по весам сложности.
   - **Progress & Gap Analytics** — радар-чарты освоения тем, выявление пробелов (`masteryScore < 60%`) и автоматическая рекомендация повышения уровня CEFR (`>= 80%` по всем темам уровня).
   - **Educator Portal** — управление учебными группами (до 200 студентов), деплой заданий, ручная корректировка AI-оценок (*Teacher Override*) и экспорт отчётов в **PDF / CSV**.

2. **Безопасность, аутентификация и приватность (Security by Design)**:
   - **Двойная аутентификация (Email + Google OAuth2)** — поддержка классического входа/регистрации по электронной почте и паролю (`POST /api/auth/login`, `POST /api/auth/register`) и быстрого входа через **Google OAuth2 (Google Identity Services)** (`POST /api/auth/google`) с проверкой ID-токена (`email_verified`, `aud`) через Google `tokeninfo` API и автоматическим созданием профиля при первом входе.
   - **In-Memory Access JWT (15 мин)** + **HttpOnly Strict Refresh Cookie (30 дней)** с хранением SHA-256 хешей сессий в Redis.
   - **AES-256-GCM шифрование** пользовательских BYOK API-ключей (12-байтный случайный IV + 128-битный тег аутентификации).
   - **Приватный групповой лидерборд** — отсутствие глобального лидерборда; рейтинг рассчитывается только внутри учебной группы преподавателя под псевдонимами (`displayAlias`).
   - **Soft Delete в группах** (`is_active = false`, `removed_at`) — при удалении студента из группы его старые работы скрываются от учителя, а при повторном добавлении история полностью восстанавливается.
   - **GDPR Right to Erasure** (`DELETE /api/users/me`) — полное удаление PII и анонимизация истории отправок.

---

## ⚙️ Конфигурация переменных окружения (`.env`)

В корне проекта расположен шаблон [`.env.example`](./.env.example) и рабочий файл `.env` (добавлен в `.gitignore`). Все переменные подтягиваются автоматически как в **Backend** (`spring.config.import` в `application.yml`), так и в **Frontend** (`envDir: '..'` в `vite.config.ts`) и **Docker Compose**:

- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `REDIS_HOST`, `REDIS_PORT`
- `SERVER_PORT`, `CORS_ORIGINS`, `VITE_API_URL`
- `JWT_SECRET`, `ENCRYPTION_KEY`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `VITE_GOOGLE_CLIENT_ID` *(для входа через Google OAuth2 укажите ваш OAuth 2.0 Client ID из Google Cloud Console в `GOOGLE_CLIENT_ID` и `VITE_GOOGLE_CLIENT_ID`, а также секрет в `GOOGLE_CLIENT_SECRET`)*
- `GROQ_API_KEY`, `GEMINI_API_KEY`
- `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`


---

## 🛠 Быстрый старт и сборка

### 1. Запуск через Docker Compose
```bash
docker compose up --build -d
```
- **Frontend (Nginx PWA)**: `http://localhost:5173`
- **Backend API (Spring Boot)**: `http://localhost:8080/api`
- **Adminer (DB UI)**: `http://localhost:8081`

### 2. Локальная разработка и тестирование
```bash
# Backend: запуск 222 юнит-тестов и проверка покрытия JaCoCo (100.00% instructions, 100.00% branches, 100.00% lines, 100.00% methods, 100.00% classes)
cd backend && ./gradlew test jacocoTestCoverageVerification

# Frontend: запуск Vitest тестов и production-сборка
cd frontend && npm install && npm test && npm run build
```

### 3. Компиляция документации (Doxygen & Javadoc)
```bash
# Генерация полной HTML-документации (Backend + Frontend + Markdown + Presentation)
doxygen Doxyfile

# Генерация Javadoc для Backend
cd backend && ./gradlew javadoc
```
Скомпилированная документация сохраняется в `docs/generated/html/index.html` (папка `docs/generated/` добавлена в `.gitignore`).
