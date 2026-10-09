/**
 * @file LinguaOptimaApplication.java
 * @brief Spring Boot entry point for the Lingua Optima backend application.
 *
 * @package com
 * @brief Root organization namespace.
 *
 * @package com.linguaoptima
 * @brief Root package for the Lingua Optima platform.
 *
 * @package com.linguaoptima.api
 * @brief Core backend API root package.
 *
 * @package com.linguaoptima.api.config
 * @brief Security, JWT filter, CORS, Redis, Web multipart, and scheduling configuration.
 *
 * @package com.linguaoptima.api.controller
 * @brief REST API controllers for authentication, tasks, CAT sessions, OCR submissions, groups, progress, notifications, subscriptions, API keys, leaderboards, and report exports.
 *
 * @package com.linguaoptima.api.domain
 * @brief JPA persistence entities representing users, tasks, questions, assignments, sessions, submissions, groups, progress, notifications, subscriptions, usage counters, and API keys.
 *
 * @package com.linguaoptima.api.domain.enums
 * @brief Domain enumeration types (Role, CefrLevel, TaskType, DifficultyLevel, SubmissionType, AssignmentStatus, SubscriptionTier, AIProvider, NotificationType, PaymentErrorCode).
 *
 * @package com.linguaoptima.api.dto
 * @brief Data Transfer Objects (DTOs) for REST API requests and responses.
 *
 * @package com.linguaoptima.api.dto.request
 * @brief Inbound request DTO payloads with Jakarta Bean Validation constraints.
 *
 * @package com.linguaoptima.api.dto.response
 * @brief Outbound response DTO projections returned by REST controllers.
 *
 * @package com.linguaoptima.api.exception
 * @brief Custom domain exceptions and the centralized GlobalExceptionHandler.
 *
 * @package com.linguaoptima.api.repository
 * @brief Spring Data JPA repositories for PostgreSQL persistence and custom JPQL queries.
 *
 * @package com.linguaoptima.api.scheduler
 * @brief Scheduled CRON jobs for daily streak checks, weekly quota resets, and contextual grammar notifications.
 *
 * @package com.linguaoptima.api.service
 * @brief Core business logic services for authentication, adaptive testing (CAT), Zero-Retention OCR, essay scoring, gamification, groups, subscriptions, encryption, and exports.
 *
 * @package com.linguaoptima.api.service.ai
 * @brief AI Broker and LLM provider integrations (Groq Llama 3.1, Google Gemini 1.5 Flash, OpenAI, Anthropic) with Redis caching and fallback routing.
 *
 * @package com.linguaoptima.api.util
 * @brief Utility registries and structured AI prompt templates (CefrTopicRegistry, PromptTemplates).
 *
 * @package frontend
 * @brief React 18 + TypeScript + Vite Progressive Web App (PWA) client service.
 *
 * @package frontend.api
 * @brief Axios HTTP client modules, JWT silent-refresh interceptors, and backend REST API bindings.
 *
 * @package frontend.components
 * @brief Reusable UI components grouped by domain role (auth, common, student, teacher).
 *
 * @package frontend.components.auth
 * @brief Authentication views supporting Email/Password and Google OAuth2 Identity Services.
 *
 * @package frontend.components.common
 * @brief Shared layout, navigation, RBAC route guards, modals, and PWA offline indicators.
 *
 * @package frontend.components.student
 * @brief Student workspace modules (Self-Service Task Generator, CAT Adaptive Session, Zero-Retention OCR upload, Essay Editor, AI Review, Radar Progress, Group Leaderboard).
 *
 * @package frontend.components.teacher
 * @brief Educator portal modules (Teacher Dashboard, Student Cohorts, Task Deployment, Submission Override Review, PDF/CSV Report Exports).
 *
 * @package frontend.hooks
 * @brief Custom React hooks for authentication, real-time SSE notifications, quota tracking, offline status, debouncing, and auto-saved drafts.
 *
 * @package frontend.pages
 * @brief Top-level route page wrappers for Landing, Student Portal, Educator Portal, Profile, and Subscriptions.
 *
 * @package frontend.store
 * @brief Zustand state management stores (authStore, notificationStore, taskStore, uiStore).
 *
 * @package frontend.types
 * @brief TypeScript domain interfaces and API contract definitions.
 *
 * @package frontend.utils
 * @brief Client utilities for CEFR badge styling, date formatting, essay word counting, and IndexedDB offline synchronization.
 */
package com.linguaoptima.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @brief Spring Boot entry point for the Lingua Optima backend application.
 */
@SpringBootApplication
public class LinguaOptimaApplication {

    /**
     * @brief Main method initiating the Spring ApplicationContext and web server.
     * @param args Command line arguments passed at application startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(LinguaOptimaApplication.class, args);
    }
}
