# Lingua Optima - Documentation & Doxygen Refactoring Plan

## Goals
- Remove all informal/plain comments (`// ...`) across all codebase files.
- Add structured Doxygen/Javadoc comments with `@brief`, `@param`, `@return`, `@throws` for every class, interface, method, and function.
- Ensure `./gradlew javadoc` and `doxygen Doxyfile` compile cleanly.
- Verify JaCoCo test verification (>= 95%) and frontend tests.
- Commit in logical stages.

## File Inventory & Progress Tracker

### Stage 1: Backend Controllers & Infrastructure
- [ ] `AuthController.java`
- [ ] `UserController.java`
- [ ] `TaskController.java`
- [ ] `SessionController.java`
- [ ] `SubmissionController.java`
- [ ] `GroupController.java`
- [ ] `ProgressController.java`
- [ ] `NotificationController.java`
- [ ] `SubscriptionController.java`
- [ ] `ApiKeyController.java`
- [ ] `LeaderboardController.java`
- [ ] `ExportController.java`
- [ ] `GlobalExceptionHandler.java`
- [ ] `CefrTopicRegistry.java`
- [ ] `PromptTemplates.java`

### Stage 2: Backend Core Services & AI Providers
- [ ] `AuthService.java`
- [ ] `JwtService.java`
- [ ] `UserService.java`
- [ ] `TaskService.java`
- [ ] `SessionService.java`
- [ ] `SubmissionService.java`
- [ ] `ScoringService.java`
- [ ] `OcrService.java`
- [ ] `ProgressService.java`
- [ ] `GroupService.java`
- [ ] `NotificationService.java`
- [ ] `GamificationService.java`
- [ ] `SubscriptionService.java`
- [ ] `PaymentService.java`
- [ ] `UsageService.java`
- [ ] `ExportService.java`
- [ ] `ApiKeyService.java`
- [ ] `EncryptionService.java`
- [ ] `LeaderboardService.java`
- [ ] `AIBrokerService.java`
- [ ] `GroqProvider.java`
- [ ] `GeminiProvider.java`
- [ ] `OpenAIProvider.java`
- [ ] `AnthropicProvider.java`

### Stage 3: Backend Schedulers, Security & Configuration
- [ ] `StreakScheduler.java`
- [ ] `UsageResetScheduler.java`
- [ ] `NotificationScheduler.java`
- [ ] `AiQueueScheduler.java`
- [ ] `SecurityConfig.java`
- [ ] `JwtAuthenticationFilter.java`
- [ ] `RedisConfig.java`
- [ ] `CorsConfig.java`

### Stage 4: Frontend API Layer, Utilities, Stores & Hooks
- [ ] `axiosInstance.ts`
- [ ] `authApi.ts`
- [ ] `taskApi.ts`
- [ ] `sessionApi.ts`
- [ ] `submissionApi.ts`
- [ ] `progressApi.ts`
- [ ] `groupApi.ts`
- [ ] `notificationApi.ts`
- [ ] `subscriptionApi.ts`
- [ ] `apiKeyApi.ts`
- [ ] `exportApi.ts`
- [ ] `leaderboardApi.ts`
- [ ] `formatDate.ts`
- [ ] `cefrColors.ts`
- [ ] `wordCount.ts`
- [ ] `offlineSync.ts`
- [ ] `authStore.ts`
- [ ] `notificationStore.ts`
- [ ] `taskStore.ts`
- [ ] `uiStore.ts`
- [ ] `useAuth.ts`
- [ ] `useOnline.ts`
- [ ] `useSSE.ts`
- [ ] `useUsage.ts`
- [ ] `useDraft.ts`

### Stage 5: Frontend Components & Pages
- [ ] Common Components (`Navbar`, `Footer`, `CefrBadge`, `LoadingSpinner`, `OfflineBanner`, `UpgradeWall`, `RoleGuard`, `ProtectedRoute`, `Toast`, `ConfirmDialog`, `ErrorBoundary`)
- [ ] Student Components (`Dashboard`, `GenerateTask`, `TaskView`, `AdaptiveSession`, `OcrSubmit`, `EssayEditor`, `AIReview`, `MyUnits`, `Progress`, `GroupLeaderboard`, `LevelUpModal`)
- [ ] Teacher Components (`TeacherDashboard`, `StudentGroups`, `ConfigureTask`, `SubmissionsReview`, `ExportReports`)
- [ ] Auth Components (`LoginPage`, `ForgotPassword`)
- [ ] Pages (`Landing`, `StudentApp`, `TeacherApp`, `ProfilePage`, `SubscriptionPage`, `NotFound`, `App`, `main`)
