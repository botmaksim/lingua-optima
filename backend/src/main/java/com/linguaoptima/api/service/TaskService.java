/**
 * @file TaskService.java
 * @brief Service responsible for AI-powered educational task generation, template management, and task assignments.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.domain.enums.TaskType;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.CreateCustomTaskRequest;
import com.linguaoptima.api.dto.request.CustomQuestionRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.dto.response.TopicsCatalogResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import com.linguaoptima.api.service.ai.AIBrokerService;
import com.linguaoptima.api.util.CefrTopicRegistry;
import com.linguaoptima.api.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Service responsible for AI-powered educational task generation, template management, and task assignments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    /** @brief Field representing task repository in TaskService. */
    private final TaskRepository taskRepository;
    /** @brief Field representing task assignment repository in TaskService. */
    private final TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Field representing group repository in TaskService. */
    private final GroupRepository groupRepository;
    /** @brief Field representing group student repository in TaskService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing ai broker service in TaskService. */
    private final AIBrokerService aiBrokerService;
    /** @brief Field representing subscription service in TaskService. */
    private final SubscriptionService subscriptionService;
    /** @brief Field representing usage service in TaskService. */
    private final UsageService usageService;
    /** @brief Field representing notification service in TaskService. */
    private final NotificationService notificationService;
    /** @brief Field representing object mapper in TaskService. */
    private final ObjectMapper objectMapper;
    /** @brief Field representing curriculum storage service for context injection. */
    private final CurriculumStorageService curriculumStorageService;
    /** @brief Field representing submission repository for tracking student attempts. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field enforcing single device and concurrency protection during generation. */
    private final GenerationProtectionService generationProtectionService;
    /** @brief Pricing properties for tiered quota validation. */
    private final PricingProperties pricingProperties;

    /**
     * @brief Generates an educational task with AI, persists it, and creates an automatic assignment for students.
     * @param params Generation parameters including CEFR level, grammar topic, task type, and question count.
     * @param user User initiating the generation request.
     * @return TaskResponse DTO containing task questions and metadata.
     * @throws ForbiddenException if user tier does not allow requested CEFR level.
     * @throws com.linguaoptima.api.exception.QuotaExceededException if free weekly quota is exhausted.
     */
    @Transactional
    public TaskResponse generateTask(TaskParamsRequest params, User user) {
        return generateTask(params, user, null);
    }

    /**
     * @brief Generates an educational task with AI with explicit client device tracking.
     * @param params Generation parameters.
     * @param user User initiating the generation request.
     * @param deviceId Client device identifier.
     * @return TaskResponse DTO.
     */
    @Transactional
    public TaskResponse generateTask(TaskParamsRequest params, User user, String deviceId) {
        subscriptionService.validateCefrLevelAccess(user, params.getCefrLevel());
        if (generationProtectionService != null && user != null) {
            generationProtectionService.verifyDevice(user.getId(), deviceId);
            generationProtectionService.acquireGenerationLock(user.getId(), deviceId);
        }
        try {
            usageService.incrementEvaluation(user);

            String prompt = buildPromptFromParams(params);
            user.setPreferredProvider(params.getProvider());
            user.setPreferredModel(params.getModelName());

            String rawJson = aiBrokerService.generateTaskContent(prompt, user);
            long estimatedTokens = UsageService.estimateTokens(prompt) + UsageService.estimateTokens(rawJson);
            usageService.consumeTokens(user, estimatedTokens);

            Task task = parseAndBuildTask(rawJson, params, user, false);
            Task savedTask = taskRepository.save(task);

            if (user.getRole() == Role.STUDENT) {
                TaskAssignment selfAssignment = TaskAssignment.builder()
                    .task(savedTask)
                    .student(user)
                    .assignedBy(user)
                    .status(AssignmentStatus.IN_PROGRESS)
                    .maxAttempts(0)
                    .attemptsUsed(0)
                    .createdAt(LocalDateTime.now())
                    .build();
                taskAssignmentRepository.save(selfAssignment);
                log.info("Created self-service assignment for student {}", user.getEmail());
            }

            return TaskResponse.fromEntity(savedTask);
        } finally {
            if (generationProtectionService != null && user != null) {
                generationProtectionService.releaseGenerationLock(user.getId());
            }
        }
    }

    /**
     * @brief Generates a transient task preview without persisting records in the database.
     * @param params Generation parameters.
     * @param user User requesting the preview.
     * @return TaskResponse DTO containing preview questions.
     */
    public TaskResponse previewTask(TaskParamsRequest params, User user) {
        return previewTask(params, user, null);
    }

    /**
     * @brief Generates a transient task preview with explicit client device tracking.
     * @param params Generation parameters.
     * @param user User requesting the preview.
     * @param deviceId Client device identifier.
     * @return TaskResponse DTO containing preview questions.
     */
    public TaskResponse previewTask(TaskParamsRequest params, User user, String deviceId) {
        subscriptionService.validateCefrLevelAccess(user, params.getCefrLevel());
        if (generationProtectionService != null && user != null) {
            generationProtectionService.verifyDevice(user.getId(), deviceId);
            generationProtectionService.acquireGenerationLock(user.getId(), deviceId);
        }
        try {
            String prompt = buildPromptFromParams(params);
            user.setPreferredProvider(params.getProvider());
            user.setPreferredModel(params.getModelName());

            String rawJson = aiBrokerService.generateTaskContent(prompt, user);
            Task task = parseAndBuildTask(rawJson, params, user, false);
            task.setId(UUID.randomUUID());
            return TaskResponse.fromEntity(task);
        } finally {
            if (generationProtectionService != null && user != null) {
                generationProtectionService.releaseGenerationLock(user.getId());
            }
        }
    }

    /**
     * @brief Generates and persists a task marked as a reusable educator template.
     * @param params Generation parameters.
     * @param teacher Educator creating the template.
     * @return TaskResponse DTO of the saved template task.
     * @throws ForbiddenException if caller does not possess educator or administrator privileges.
     */
    @Transactional
    public TaskResponse saveAsTemplate(TaskParamsRequest params, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can save tasks as templates.");
        }

        String prompt = buildPromptFromParams(params);
        teacher.setPreferredProvider(params.getProvider());
        teacher.setPreferredModel(params.getModelName());

        String rawJson = aiBrokerService.generateTaskContent(prompt, teacher);
        Task task = parseAndBuildTask(rawJson, params, teacher, true);
        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    /**
     * @brief Creates and persists a custom educator-configured task with explicit questions and optional cohort assignment.
     * @param request Custom task specification payload.
     * @param teacher Authenticated educator principal.
     * @return TaskResponse DTO of the persisted task.
     * @throws ForbiddenException if user is not an educator or administrator.
     */
    @Transactional
    @lombok.SneakyThrows
    public TaskResponse createCustomTask(CreateCustomTaskRequest request, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can create custom tasks.");
        }

        subscriptionService.validateCefrLevelAccess(teacher, request.getCefrLevel());

        String topic = (request.getGrammarTopic() != null && !request.getGrammarTopic().isBlank())
            ? request.getGrammarTopic().trim()
            : "Custom Topic";
        String domain = (request.getDomain() != null && !request.getDomain().isBlank())
            ? request.getDomain().trim()
            : "General";
        DifficultyLevel diff = request.getDifficulty() != null
            ? request.getDifficulty()
            : DifficultyLevel.MEDIUM;

        int sumPoints = 0;
        for (CustomQuestionRequest qReq : request.getQuestions()) {
            sumPoints += qReq.getPoints() > 0 ? qReq.getPoints() : 10;
        }
        int effectiveTotal = (request.getTotalPoints() != null && request.getTotalPoints() > 0)
            ? request.getTotalPoints()
            : (sumPoints > 0 ? sumPoints : 100);

        Task task = Task.builder()
            .type(request.getTaskType())
            .cefrLevel(request.getCefrLevel())
            .grammarTopic(topic)
            .domain(domain)
            .difficulty(diff)
            .totalPoints(effectiveTotal)
            .content(request.getContent() != null ? request.getContent().trim() : "")
            .createdBy(teacher)
            .isTemplate(request.isTemplate())
            .createdAt(LocalDateTime.now())
            .build();

        List<Map<String, Object>> answerKeyList = new ArrayList<>();
        List<TaskQuestion> questions = new ArrayList<>();
        int order = 1;

        for (CustomQuestionRequest qReq : request.getQuestions()) {
            int currentOrder = qReq.getQuestionOrder() > 0 ? qReq.getQuestionOrder() : order++;
            String optionsJson = "[]";
            if (qReq.getOptions() != null && !qReq.getOptions().isEmpty() &&
                request.getTaskType() != TaskType.REWRITE && request.getTaskType() != TaskType.OPEN_BRACKETS) {
                optionsJson = objectMapper.writeValueAsString(qReq.getOptions());
            }

            int diffVal = qReq.getDifficulty() > 0 ? qReq.getDifficulty() : 2;
            int qPoints = qReq.getPoints() > 0 ? qReq.getPoints() : Math.max(1, effectiveTotal / Math.max(1, request.getQuestions().size()));
            String ruleVal = (qReq.getGrammarRule() != null && !qReq.getGrammarRule().isBlank())
                ? qReq.getGrammarRule().trim()
                : task.getGrammarTopic();

            TaskQuestion tq = TaskQuestion.builder()
                .task(task)
                .questionOrder(currentOrder)
                .questionText(qReq.getQuestionText().trim())
                .correctAnswer(qReq.getCorrectAnswer().trim())
                .optionsJson(optionsJson)
                .difficulty(diffVal)
                .points(qPoints)
                .grammarRule(ruleVal)
                .build();
            questions.add(tq);

            Map<String, Object> akItem = new HashMap<>();
            akItem.put("questionOrder", currentOrder);
            akItem.put("correctOption", qReq.getCorrectAnswer().trim());
            akItem.put("correctAnswer", qReq.getCorrectAnswer().trim());
            answerKeyList.add(akItem);
        }

        task.setAnswerKey(objectMapper.writeValueAsString(answerKeyList));
        task.setQuestions(questions);
        Task saved = taskRepository.save(task);

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            AssignTaskRequest assignReq = AssignTaskRequest.builder()
                .groupIds(request.getGroupIds())
                .dueDate(request.getDueDate())
                .maxAttempts(request.getMaxAttempts())
                .build();
            assignTask(saved.getId(), assignReq, teacher);
        }

        return TaskResponse.fromEntity(saved);
    }

    /**
     * @brief Constructs an AI task generation prompt applying safe defaults for optional parameters.
     * @param params Task generation parameters.
     * @return Formatted prompt string for the AI provider.
     */
    private String buildPromptFromParams(TaskParamsRequest params) {
        String targetRule = params.getCustomRule();
        String targetVocab = params.getCustomVocabulary();

        if (Boolean.TRUE.equals(params.getEcoMode())) {
            targetRule = null;
            targetVocab = null;
        } else if (curriculumStorageService != null) {
            String[] resolvedContext = curriculumStorageService.resolvePromptCurriculumContext(
                params.getCefrLevel(),
                params.getGrammarTopic(),
                params.getCustomRule(),
                params.getCustomVocabulary()
            );
            if (resolvedContext != null) {
                targetRule = resolvedContext[0];
                targetVocab = resolvedContext[1];
            }
        }

        return PromptTemplates.buildTaskGenerationPrompt(
            params.getCefrLevel().name(),
            params.getGrammarTopic() != null ? params.getGrammarTopic() : "General",
            params.getDomain() != null ? params.getDomain() : "Daily Life",
            params.getTaskType().name(),
            params.getDifficulty() != null ? params.getDifficulty().name() : DifficultyLevel.MEDIUM.name(),
            params.getNumberOfQuestions() > 0 ? params.getNumberOfQuestions() : 5,
            targetRule,
            targetVocab
        );
    }


    /**
     * @brief Assigns a task to all active students across one or more teacher groups.
     * @param taskId Unique identifier of the task.
     * @param request Assignment details including group IDs and optional due date.
     * @param teacher Educator assigning the task.
     * @throws ForbiddenException if caller is not an educator or does not own the target group.
     * @throws ResourceNotFoundException if task or target group does not exist.
     */
    @Transactional
    public void assignTask(UUID taskId, AssignTaskRequest request, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can assign tasks.");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        int effectiveMaxAttempts = (request.getMaxAttempts() == null || request.getMaxAttempts() < 0)
            ? 1
            : request.getMaxAttempts();

        SubscriptionTier teacherTier = usageService != null ? usageService.getUserTier(teacher) : SubscriptionTier.FREE;
        int maxAllowedGroups = pricingProperties != null ? pricingProperties.getTierConfig(teacherTier).getMaxGroups() : 1;
        List<Group> teacherGroups = groupRepository.findByTeacher(teacher);

        for (UUID groupId : request.getGroupIds()) {
            Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

            if (!group.getTeacher().getId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not the owner of group: " + group.getName());
            }

            int groupIdx = -1;
            for (int i = 0; i < teacherGroups.size(); i++) {
                if (teacherGroups.get(i).getId().equals(groupId)) {
                    groupIdx = i;
                    break;
                }
            }
            if (groupIdx >= maxAllowedGroups) {
                throw new com.linguaoptima.api.exception.QuotaExceededException("Group '" + group.getName() + "' is in read-only mode because your " +
                    (teacherTier != null ? teacherTier.name() : "FREE") +
                    " plan allows up to " + maxAllowedGroups + " active group(s). Upgrade to Educator Pro to assign tasks to this cohort.");
            }

            List<GroupStudent> students = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);
            for (GroupStudent gs : students) {
                User student = gs.getStudent();
                TaskAssignment assignment = TaskAssignment.builder()
                    .task(task)
                    .student(student)
                    .assignedBy(teacher)
                    .dueDate(request.getDueDate())
                    .status(AssignmentStatus.PENDING)
                    .maxAttempts(effectiveMaxAttempts)
                    .attemptsUsed(0)
                    .createdAt(LocalDateTime.now())
                    .build();
                taskAssignmentRepository.save(assignment);

                notificationService.send(student,
                    "📝 New task assigned: " + task.getType() + " (" + task.getCefrLevel() + ") by " + teacher.getFullName(),
                    NotificationType.TASK);
            }
        }
    }

    /**
     * @brief Retrieves all tasks accessible to the specified user with assignment and attempt metadata.
     * @param user User requesting accessible tasks.
     * @return List of TaskResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForUser(User user) {
        return taskRepository.findAllAccessibleForUser(user.getId()).stream()
            .map(task -> enrichTaskForUser(task, user))
            .collect(Collectors.toList());
    }

    /**
     * @brief Assembles and returns comprehensive syllabus topics catalog including mixed challenges and domains.
     * @return TopicsCatalogResponse populated with level topics, mixed topics, and domains.
     */
    public TopicsCatalogResponse getTopicsCatalog() {
        Map<com.linguaoptima.api.domain.enums.CefrLevel, List<String>> core = new LinkedHashMap<>();
        Map<com.linguaoptima.api.domain.enums.CefrLevel, List<String>> mixed = new LinkedHashMap<>();
        for (com.linguaoptima.api.domain.enums.CefrLevel level : com.linguaoptima.api.domain.enums.CefrLevel.values()) {
            core.put(level, CefrTopicRegistry.getTopicsForLevel(level));
            mixed.put(level, CefrTopicRegistry.getMixedTopicsForLevel(level));
        }

        return TopicsCatalogResponse.builder()
            .topicsByLevel(core)
            .mixedTopicsByLevel(mixed)
            .crossLevelTopics(CefrTopicRegistry.getThematicMixedTopics())
            .domains(CefrTopicRegistry.getCommonDomains())
            .build();
    }

    /**
     * @brief Retrieves a specific task by its unique identifier.
     * @param taskId Unique identifier of the task.
     * @return TaskResponse DTO containing full task details and questions.
     * @throws ResourceNotFoundException if task cannot be found.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        return getTaskById(taskId, null);
    }

    /**
     * @brief Retrieves a specific task by its unique identifier enriched with student assignment metadata.
     * @param taskId Unique identifier of the task.
     * @param user Optional user requesting the task to resolve assignment status and attempt limits.
     * @return TaskResponse DTO containing full task details, questions, and assignment state.
     * @throws ResourceNotFoundException if task cannot be found.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        return enrichTaskForUser(task, user);
    }

    /**
     * @brief Enriches a TaskResponse with student-specific assignment status, attempt limits, and latest submission ID.
     * @param task The domain Task entity.
     * @param user The student user (optional).
     * @return Enriched TaskResponse DTO.
     */
    private TaskResponse enrichTaskForUser(Task task, User user) {
        TaskResponse response = TaskResponse.fromEntity(task);
        if (user != null) {
            taskAssignmentRepository.findByStudentIdAndTaskId(user.getId(), task.getId())
                .ifPresent(assignment -> {
                    response.setAssignmentId(assignment.getId());
                    if (assignment.getAssignedBy() != null && !assignment.getAssignedBy().getId().equals(user.getId())) {
                        response.setAssignedByName(assignment.getAssignedBy().getFullName());
                    }
                    response.setDueDate(assignment.getDueDate());
                    AssignmentStatus st = assignment.getStatus() != null ? assignment.getStatus() : AssignmentStatus.PENDING;
                    response.setAssignmentStatus(st.name());
                    int maxAtt = assignment.getMaxAttempts() != null ? assignment.getMaxAttempts() : 1;
                    int usedAtt = assignment.getAttemptsUsed();
                    if (usedAtt == 0 && (st == AssignmentStatus.SUBMITTED || st == AssignmentStatus.GRADED)) {
                        usedAtt = 1;
                    }
                    response.setMaxAttempts(maxAtt);
                    response.setAttemptsUsed(usedAtt);
                    response.setCanSubmit(maxAtt <= 0 || usedAtt < maxAtt);
                    if (submissionRepository != null) {
                        submissionRepository.findByAssignmentId(assignment.getId()).stream()
                            .max(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                            .ifPresent(sub -> response.setLatestSubmissionId(sub.getId()));
                    }
                });
        }
        return response;
    }

    /**
     * @brief Parses AI-generated JSON into structured Task and TaskQuestion entities.
     * @param rawJson Raw JSON string returned from AI model.
     * @param params Generation request parameters.
     * @param user User initiating the generation.
     * @param isTemplate Flag indicating if the task is an educator template.
     * @return Populated Task entity.
     */
    private Task parseAndBuildTask(String rawJson, TaskParamsRequest params, User user, boolean isTemplate) {
        int reqTotal = (params.getTotalPoints() != null && params.getTotalPoints() > 0)
            ? params.getTotalPoints()
            : 100;

        Task task = Task.builder()
            .type(params.getTaskType())
            .cefrLevel(params.getCefrLevel())
            .grammarTopic(params.getGrammarTopic())
            .domain(params.getDomain())
            .difficulty(params.getDifficulty() != null ? params.getDifficulty() : DifficultyLevel.MEDIUM)
            .totalPoints(reqTotal)
            .createdBy(user)
            .isTemplate(isTemplate)
            .createdAt(LocalDateTime.now())
            .build();

        try {
            String cleanedJson = cleanJson(rawJson);
            JsonNode root = objectMapper.readTree(cleanedJson);
            String rawContent = root.path("content").asText("");
            task.setContent(sanitizeTaskContent(rawContent, params));
            JsonNode answerKeyNode = root.path("answerKey");
            task.setAnswerKey(answerKeyNode.toString());

            java.util.Map<Integer, String> answerKeyMap = new java.util.HashMap<>();
            if (answerKeyNode.isArray()) {
                int idx = 1;
                for (JsonNode ak : answerKeyNode) {
                    int qOrder = ak.path("questionOrder").asInt(ak.path("questionId").asInt(idx++));
                    String opt = ak.path("correctOption").asText(ak.path("correctAnswer").asText(""));
                    if (!opt.isBlank()) {
                        answerKeyMap.put(qOrder, opt);
                    }
                }
            }

            JsonNode questionsNode = root.path("questions");
            List<TaskQuestion> questions = new ArrayList<>();
            if (questionsNode.isArray()) {
                int qCount = Math.max(1, questionsNode.size());
                int basePoints = reqTotal / qCount;
                int remainder = reqTotal % qCount;
                int order = 1;
                for (JsonNode qNode : questionsNode) {
                    int currentOrder = order++;
                    JsonNode optionsNode = qNode.path("options");
                    String optionsJson = optionsNode.isMissingNode() ? "[]" : optionsNode.toString();
                    if (params.getTaskType() == TaskType.REWRITE || params.getTaskType() == TaskType.OPEN_BRACKETS) {
                        optionsJson = "[]";
                    }

                    String resolvedAnswer = qNode.path("correctAnswer").asText(
                        qNode.path("correctOption").asText(
                            answerKeyMap.getOrDefault(currentOrder, "Answer")
                        )
                    );

                    int qPoints = basePoints + (currentOrder == 1 ? remainder : 0);

                    TaskQuestion tq = TaskQuestion.builder()
                        .id(UUID.randomUUID())
                        .task(task)
                        .questionOrder(currentOrder)
                        .questionText(qNode.path("text").asText("Question text"))
                        .correctAnswer(resolvedAnswer)
                        .optionsJson(optionsJson)
                        .difficulty(qNode.path("difficulty").asInt(2))
                        .points(qPoints > 0 ? qPoints : 10)
                        .grammarRule(qNode.path("grammarRule").asText(params.getGrammarTopic()))
                        .build();
                    questions.add(tq);
                }
            }
            task.setQuestions(questions);
        } catch (Exception e) {
            log.warn("Error parsing task JSON, using defaults: {}", e.getMessage());
            task.setContent("Practice exercise for " + params.getCefrLevel());
            task.setAnswerKey("[]");
            task.setTotalPoints(reqTotal);
        }

        return task;
    }

    private static final List<String> PROMPT_LEAK_MARKERS = List.of(
        "generate an english",
        "cefr level:",
        "grammar topic:",
        "domain/context:",
        "task type:",
        "return only a valid json",
        "critical: do not echo",
        "number of questions:",
        "system parameters",
        "overall instructions or context passage"
    );

    private static final List<String> ESSAY_INAPPROPRIATE_MARKERS = List.of(
        "numbered blank",
        "choose the correct",
        "fill in the blank",
        "fill in the blanks",
        "multiple choice",
        "select the correct"
    );

    /**
     * @brief Strips markdown code fence blocks from AI response strings.
     * @param rawJson Raw AI response string.
     * @return Clean JSON string.
     */
    private String cleanJson(String rawJson) {
        if (rawJson == null) {
            return "{}";
        }
        return rawJson.trim()
            .replaceFirst("^```(?:json)?\\s*", "")
            .replaceFirst("\\s*```$", "")
            .trim();
    }

    /**
     * @brief Inspects and sanitizes task content to prevent leaking AI meta-prompts or instructions.
     * @param rawContent Raw content returned from the AI model.
     * @param params Generation parameters used to construct contextual fallback content.
     * @return Sanitized student-facing task instructions or topic.
     */
    private String sanitizeTaskContent(String rawContent, TaskParamsRequest params) {
        if (rawContent.isBlank() || containsPromptLeak(rawContent)) {
            return generateDefaultTaskContent(params);
        }
        if (params.getTaskType() == TaskType.ESSAY && isExerciseLikeForEssay(rawContent)) {
            return generateDefaultTaskContent(params);
        }
        return rawContent.trim();
    }

    /**
     * @brief Checks if essay instructions mistakenly contain gap-fill or multiple-choice exercise phrases.
     * @param text Text to evaluate.
     * @return True if exercise markers are detected.
     */
    private boolean isExerciseLikeForEssay(String text) {
        String lower = text.toLowerCase();
        for (String marker : ESSAY_INAPPROPRIATE_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @brief Checks if a string contains known system prompt leak keywords.
     * @param text Text to evaluate.
     * @return True if prompt leak keyword is present.
     */
    private boolean containsPromptLeak(String text) {
        String lower = text.toLowerCase();
        for (String marker : PROMPT_LEAK_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @brief Builds clean student-facing default task instructions based on task type.
     * @param params Task generation parameters.
     * @return Human-readable assignment instructions.
     */
    private String generateDefaultTaskContent(TaskParamsRequest params) {
        if (params.getTaskType() == TaskType.ESSAY) {
            String topic = (params.getGrammarTopic() != null && !params.getGrammarTopic().isBlank())
                ? params.getGrammarTopic() : "General Topic";
            String domain = (params.getDomain() != null && !params.getDomain().isBlank())
                ? params.getDomain() : "Daily Life";
            return "Write an essay discussing " + topic + " in relation to " + domain
                + ". Present clear arguments and relevant examples to support your viewpoint.";
        }
        String topic = (params.getGrammarTopic() != null && !params.getGrammarTopic().isBlank())
            ? params.getGrammarTopic() : "English grammar";
        return "Complete the following exercises focusing on " + topic + ". Read each question carefully and select the best answer.";
    }
}
