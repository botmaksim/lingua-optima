# Lingua Optima - Documentation & Doxygen Refactoring Plan

## Goals
- [x] Remove all informal/plain comments (`// ...`) across all codebase files.
- [x] Add structured Doxygen/Javadoc comments with `@file`, `@brief`, `@param`, `@return`, `@throws` for every class, interface, method, and function.
- [x] Ensure `./gradlew javadoc` and `doxygen Doxyfile` compile cleanly with zero warnings/errors.
- [x] Add compiled documentation output directories (`docs/generated/`, `doxygen_output/`, `backend/build/`) to `.gitignore`.
- [x] Ensure root documentation (`docs/README.md`) links all documentation files (`BACKEND.md`, `FRONTEND.md`, `AI_INTEGRATION.md`, `DEVOPS.md`) and opens `presentation.html`.
- [x] Configure `.env` (gitignored) and `.env.example` with variable names only (no values assigned) and connect both backend and frontend to pull from root `.env`.
- [x] Verify JaCoCo test coverage verification (>= 95%) and frontend tests.
- [x] Commit in logical stages.

## File Inventory & Progress Tracker (100% Completed)

### Stage 1: Backend Controllers & Infrastructure
- [x] `AuthController.java`
- [x] `UserController.java`
- [x] `TaskController.java`
- [x] `SessionController.java`
- [x] `SubmissionController.java`
- [x] `GroupController.java`
- [x] `ProgressController.java`
- [x] `NotificationController.java`
- [x] `SubscriptionController.java`
- [x] `ApiKeyController.java`
- [x] `LeaderboardController.java`
- [x] `ExportController.java`
- [x] `GlobalExceptionHandler.java`
- [x] `CefrTopicRegistry.java`
- [x] `PromptTemplates.java`

### Stage 2: Backend Core Services & AI Providers
- [x] `AuthService.java`
- [x] `JwtService.java`
- [x] `UserService.java`
- [x] `TaskService.java`
- [x] `SessionService.java`
- [x] `SubmissionService.java`
- [x] `ScoringService.java`
- [x] `OcrService.java`
- [x] `ProgressService.java`
- [x] `GroupService.java`
- [x] `NotificationService.java`
- [x] `GamificationService.java`
- [x] `SubscriptionService.java`
- [x] `PaymentService.java`
- [x] `UsageService.java`
- [x] `ExportService.java`
- [x] `ApiKeyService.java`
- [x] `EncryptionService.java`
- [x] `LeaderboardService.java`
- [x] `AIBrokerService.java`
- [x] `GroqProvider.java`
- [x] `GeminiProvider.java`
- [x] `OpenAIProvider.java`
- [x] `AnthropicProvider.java`

### Stage 3: Backend Schedulers, Security, Config, Domain, Enums, Repositories & DTOs
- [x] `StreakScheduler.java`
- [x] `UsageResetScheduler.java`
- [x] `NotificationScheduler.java`
- [x] `SecurityConfig.java`
- [x] `JwtAuthenticationFilter.java`
- [x] `RedisConfig.java`
- [x] `CorsConfig.java`
- [x] `WebConfig.java`
- [x] `SchedulingConfig.java`
- [x] All 14 Domain Entities (`User`, `Task`, `TaskQuestion`, `TaskAssignment`, `SessionState`, `Submission`, `Subscription`, `Group`, `GroupStudent`, `ApiKey`, `Notification`, `PendingAiTask`, `ProgressRecord`, `UsageCounter`)
- [x] All 11 Domain Enums (`AIProvider`, `AssignmentStatus`, `CefrLevel`, `DifficultyLevel`, `NotificationType`, `PaymentErrorCode`, `Role`, `SessionStatus`, `SubmissionType`, `SubscriptionTier`, `TaskType`)
- [x] All 14 Spring Data JPA Repositories (`UserRepository`, `TaskRepository`, `TaskQuestionRepository`, `TaskAssignmentRepository`, `SessionStateRepository`, `SubmissionRepository`, `SubscriptionRepository`, `GroupRepository`, `GroupStudentRepository`, `ApiKeyRepository`, `NotificationRepository`, `PendingAiTaskRepository`, `ProgressRecordRepository`, `UsageCounterRepository`)
- [x] All 14 Request DTOs & 14 Response DTOs
- [x] All 32 Backend Unit & Slice Test Suites

### Stage 4: Frontend API Layer, Types, Utilities, Stores & Hooks
- [x] `axiosInstance.ts`
- [x] `authApi.ts`
- [x] `taskApi.ts`
- [x] `sessionApi.ts`
- [x] `submissionApi.ts`
- [x] `progressApi.ts`
- [x] `groupApi.ts`
- [x] `notificationApi.ts`
- [x] `subscriptionApi.ts`
- [x] `apiKeyApi.ts`
- [x] `exportApi.ts`
- [x] `leaderboardApi.ts`
- [x] All 9 TypeScript Domain Type Modules (`user.ts`, `task.ts`, `session.ts`, `submission.ts`, `progress.ts`, `group.ts`, `leaderboard.ts`, `notification.ts`, `subscription.ts`)
- [x] `formatDate.ts`
- [x] `cefrColors.ts`
- [x] `wordCount.ts`
- [x] `offlineSync.ts`
- [x] `authStore.ts`
- [x] `notificationStore.ts`
- [x] `taskStore.ts`
- [x] `uiStore.ts`
- [x] `useAuth.ts`
- [x] `useOnline.ts`
- [x] `useSSE.ts`
- [x] `useUsage.ts`
- [x] `useDraft.ts`

### Stage 5: Frontend Components, Pages & Test Suites
- [x] Common Components (`Navbar`, `Footer`, `CefrBadge`, `LoadingSpinner`, `OfflineBanner`, `UpgradeWall`, `RoleGuard`, `ProtectedRoute`, `Toast`, `ConfirmDialog`, `ErrorBoundary`)
- [x] Student Components (`Dashboard`, `GenerateTask`, `TaskView`, `AdaptiveSession`, `OcrSubmit`, `EssayEditor`, `AIReview`, `MyUnits`, `Progress`, `GroupLeaderboard`, `LevelUpModal`)
- [x] Teacher Components (`TeacherDashboard`, `StudentGroups`, `ConfigureTask`, `SubmissionsReview`, `ExportReports`)
- [x] Auth Components (`LoginPage`, `ForgotPassword`)
- [x] Pages (`Landing`, `StudentApp`, `TeacherApp`, `ProfilePage`, `SubscriptionPage`, `NotFound`, `App`, `main`)
- [x] Frontend Test Suites (`setup.ts`, `utils.test.ts`, `stores.test.ts`, `components.test.tsx`)
