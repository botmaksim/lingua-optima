package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import com.linguaoptima.api.service.ai.AIBrokerService;
import com.linguaoptima.api.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @file TaskService.java
 * @brief Service responsible for AI-powered educational task generation, template management, and task assignments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final GroupRepository groupRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final AIBrokerService aiBrokerService;
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

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
        subscriptionService.validateCefrLevelAccess(user, params.getCefrLevel());
        usageService.incrementEvaluation(user);

        String prompt = PromptTemplates.buildTaskGenerationPrompt(
            params.getCefrLevel().name(),
            params.getGrammarTopic() != null ? params.getGrammarTopic() : "General",
            params.getDomain() != null ? params.getDomain() : "Daily Life",
            params.getTaskType().name(),
            params.getDifficulty().name(),
            params.getNumberOfQuestions() > 0 ? params.getNumberOfQuestions() : 5
        );

        String rawJson = aiBrokerService.generateTaskContent(prompt, user);
        Task task = parseAndBuildTask(rawJson, params, user, false);
        Task savedTask = taskRepository.save(task);

        if (user.getRole() == Role.STUDENT) {
            TaskAssignment selfAssignment = TaskAssignment.builder()
                .task(savedTask)
                .student(user)
                .assignedBy(user)
                .status(AssignmentStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
            taskAssignmentRepository.save(selfAssignment);
            log.info("Created self-service assignment for student {}", user.getEmail());
        }

        return TaskResponse.fromEntity(savedTask);
    }

    /**
     * @brief Generates a transient task preview without persisting records in the database.
     * @param params Generation parameters.
     * @param user User requesting the preview.
     * @return TaskResponse DTO containing preview questions.
     */
    public TaskResponse previewTask(TaskParamsRequest params, User user) {
        subscriptionService.validateCefrLevelAccess(user, params.getCefrLevel());

        String prompt = PromptTemplates.buildTaskGenerationPrompt(
            params.getCefrLevel().name(),
            params.getGrammarTopic() != null ? params.getGrammarTopic() : "General",
            params.getDomain() != null ? params.getDomain() : "Daily Life",
            params.getTaskType().name(),
            params.getDifficulty().name(),
            params.getNumberOfQuestions() > 0 ? params.getNumberOfQuestions() : 5
        );

        String rawJson = aiBrokerService.generateTaskContent(prompt, user);
        Task task = parseAndBuildTask(rawJson, params, user, false);
        task.setId(UUID.randomUUID());
        return TaskResponse.fromEntity(task);
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

        String prompt = PromptTemplates.buildTaskGenerationPrompt(
            params.getCefrLevel().name(),
            params.getGrammarTopic() != null ? params.getGrammarTopic() : "General",
            params.getDomain() != null ? params.getDomain() : "Daily Life",
            params.getTaskType().name(),
            params.getDifficulty().name(),
            params.getNumberOfQuestions() > 0 ? params.getNumberOfQuestions() : 5
        );

        String rawJson = aiBrokerService.generateTaskContent(prompt, teacher);
        Task task = parseAndBuildTask(rawJson, params, teacher, true);
        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
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

        for (UUID groupId : request.getGroupIds()) {
            Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

            if (!group.getTeacher().getId().equals(teacher.getId())) {
                throw new ForbiddenException("You are not the owner of group: " + group.getName());
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
     * @brief Retrieves all tasks accessible to the specified user.
     * @param user User requesting accessible tasks.
     * @return List of TaskResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForUser(User user) {
        return taskRepository.findAllAccessibleForUser(user.getId()).stream()
            .map(TaskResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @brief Retrieves a specific task by its unique identifier.
     * @param taskId Unique identifier of the task.
     * @return TaskResponse DTO containing full task details and questions.
     * @throws ResourceNotFoundException if task cannot be found.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        return TaskResponse.fromEntity(task);
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
        Task task = Task.builder()
            .type(params.getTaskType())
            .cefrLevel(params.getCefrLevel())
            .grammarTopic(params.getGrammarTopic())
            .domain(params.getDomain())
            .difficulty(params.getDifficulty() != null ? params.getDifficulty() : DifficultyLevel.MEDIUM)
            .createdBy(user)
            .isTemplate(isTemplate)
            .createdAt(LocalDateTime.now())
            .build();

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            task.setContent(root.path("content").asText("Task instructions and content"));
            task.setAnswerKey(root.path("answerKey").toString());

            JsonNode questionsNode = root.path("questions");
            List<TaskQuestion> questions = new ArrayList<>();
            if (questionsNode.isArray()) {
                int order = 1;
                for (JsonNode qNode : questionsNode) {
                    JsonNode optionsNode = qNode.path("options");
                    String optionsJson = optionsNode.isMissingNode() ? "[]" : optionsNode.toString();

                    TaskQuestion tq = TaskQuestion.builder()
                        .task(task)
                        .questionOrder(order++)
                        .questionText(qNode.path("text").asText("Question text"))
                        .correctAnswer(qNode.path("correctAnswer").asText("Answer"))
                        .optionsJson(optionsJson)
                        .difficulty(qNode.path("difficulty").asInt(2))
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
        }

        return task;
    }
}
