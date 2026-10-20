/**
 * @file TaskServiceTest.java
 * @brief Unit and slice test suite for TaskService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.*;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.CreateCustomTaskRequest;
import com.linguaoptima.api.dto.request.CustomQuestionRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.request.TransferTaskRequest;
import com.linguaoptima.api.dto.response.GroupTaskResponse;
import com.linguaoptima.api.dto.response.PronunciationPackResponse;
import com.linguaoptima.api.dto.response.QuestionResponse;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import com.linguaoptima.api.service.ai.AIBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for TaskService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    /** @brief Test fixture or mock dependency for task repository. */
    @Mock
    private TaskRepository taskRepository;
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Test fixture or mock dependency for group repository. */
    @Mock
    private GroupRepository groupRepository;
    /** @brief Test fixture or mock dependency for group student repository. */
    @Mock
    private GroupStudentRepository groupStudentRepository;
    /** @brief Test fixture or mock dependency for ai broker service. */
    @Mock
    private AIBrokerService aiBrokerService;
    /** @brief Test fixture or mock dependency for subscription service. */
    @Mock
    private SubscriptionService subscriptionService;
    /** @brief Test fixture or mock dependency for usage service. */
    @Mock
    private UsageService usageService;
    /** @brief Test fixture or mock dependency for notification service. */
    @Mock
    private NotificationService notificationService;
    /** @brief Test fixture or mock dependency for curriculum storage service. */
    @Mock
    private CurriculumStorageService curriculumStorageService;
    /** @brief Test fixture or mock dependency for pronunciation pack service. */
    @Mock
    private PronunciationPackService pronunciationPackService;
    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Mock for single device and concurrency generation protection. */
    @Mock
    private GenerationProtectionService generationProtectionService;
    /** @brief Mock for tiered pricing properties. */
    @Mock
    private PricingProperties pricingProperties;

    /** @brief Test fixture or mock dependency for object mapper. */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    /** @brief Test fixture or mock dependency for task service. */
    @InjectMocks
    private TaskService taskService;

    /** @brief Test fixture or mock dependency for student user. */
    private User studentUser;
    /** @brief Test fixture or mock dependency for teacher user. */
    private User teacherUser;

    /**
     * @brief Initializes test fixtures and mock state before each test in TaskServiceTest.
     */
    @BeforeEach
    void setUp() {
        studentUser = User.builder()
            .id(UUID.randomUUID())
            .email("student@lingua.com")
            .role(Role.STUDENT)
            .cefrLevel(CefrLevel.B1)
            .build();

        teacherUser = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@lingua.com")
            .fullName("Teacher Alice")
            .role(Role.TEACHER)
            .cefrLevel(CefrLevel.C1)
            .build();

        lenient().when(pricingProperties.getTierConfig(any())).thenReturn(
            PricingProperties.TierConfig.builder().maxGroups(50).build()
        );
    }

    /**
     * @brief Verifies unit test scenario: generate task student creates self assignment.
     */
    @Test
    void testGenerateTaskStudentCreatesSelfAssignment() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Conditionals")
            .domain("Business")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.MEDIUM)
            .numberOfQuestions(3)
            .build();

        String jsonResponse = """
            {
              "content": "Answer the conditional questions",
              "questions": [
                { "text": "If it rains, we ___ inside.", "options": ["stay", "will stay"], "difficulty": 2, "grammarRule": "Zero Conditional" }
              ],
              "answerKey": [{"questionOrder": 1, "correctOption": "stay"}]
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(jsonResponse);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse response = taskService.generateTask(req, studentUser);

        assertNotNull(response);
        assertEquals(CefrLevel.B1, response.getCefrLevel());
        verify(usageService).incrementEvaluation(studentUser);
        verify(subscriptionService).validateCefrLevelAccess(studentUser, CefrLevel.B1);
        verify(taskAssignmentRepository).save(argThat(assignment ->
            assignment.getStudent().getId().equals(studentUser.getId()) &&
            assignment.getAssignedBy().getId().equals(studentUser.getId())
        ));
    }

    /**
     * @brief Verifies unit test scenario: preview task does not persist.
     */
    @Test
    void testPreviewTaskDoesNotPersist() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.GAP_FILL)
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("{\"content\":\"Preview only\"}");

        TaskResponse res = taskService.previewTask(req, studentUser);
        assertNotNull(res);
        verify(taskRepository, never()).save(any());
        verify(taskAssignmentRepository, never()).save(any());
    }

    /**
     * @brief Verifies unit test scenario: save as template teacher.
     */
    @Test
    void testSaveAsTemplateTeacher() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.ESSAY)
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn("{\"content\":\"Essay prompt\"}");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.saveAsTemplate(req, teacherUser);
        assertNotNull(res);
        assertTrue(res.isTemplate());
    }

    /**
     * @brief Verifies that generating an essay task serializes target rules and vocabulary into answerKey JSON.
     */
    @Test
    void testGenerateEssayTaskWithTargetRulesAndVocabulary() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .grammarTopic("Urban Sustainability")
            .domain("Environment")
            .taskType(TaskType.ESSAY)
            .difficulty(DifficultyLevel.HARD)
            .customRule("Inverted Conditionals (Had we known...)")
            .customVocabulary("infrastructure, sprawling, mitigate")
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(
            "{\"content\":\"Write an essay on modern green cities...\",\"questions\":[]}"
        );
        final Task[] capturedTask = new Task[1];
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            capturedTask[0] = t;
            return t;
        });

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertEquals(TaskType.ESSAY, res.getType());
        assertNotNull(capturedTask[0]);
        assertTrue(capturedTask[0].getAnswerKey().contains("Inverted Conditionals"));
        assertTrue(capturedTask[0].getAnswerKey().contains("infrastructure, sprawling, mitigate"));
    }

    /**
     * @brief Verifies that creating a custom essay task serializes target rules and vocabulary into answerKey JSON.
     */
    @Test
    void testCreateCustomEssayTaskWithTargetRulesAndVocabulary() {
        CustomQuestionRequest q = CustomQuestionRequest.builder()
            .questionOrder(1)
            .questionText("Discuss the economic impact of artificial intelligence.")
            .correctAnswer("Open-ended essay response")
            .points(100)
            .build();

        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .grammarTopic("AI and Automation")
            .domain("Economy")
            .taskType(TaskType.ESSAY)
            .difficulty(DifficultyLevel.HARD)
            .content("Write a 300-word essay discussing the economic impact of AI.")
            .questions(List.of(q))
            .totalPoints(100)
            .customRule("Passive Voice with Modal Verbs")
            .customVocabulary("disruption, obsolete, productivity")
            .build();

        final Task[] capturedCustom = new Task[1];
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            capturedCustom[0] = t;
            return t;
        });

        TaskResponse res = taskService.createCustomTask(req, teacherUser);
        assertNotNull(res);
        assertEquals(TaskType.ESSAY, res.getType());
        assertNotNull(capturedCustom[0]);
        assertTrue(capturedCustom[0].getAnswerKey().contains("Passive Voice with Modal Verbs"));
        assertTrue(capturedCustom[0].getAnswerKey().contains("disruption, obsolete, productivity"));
    }

    /**
     * @brief Verifies unit test scenario: save as template non teacher throws.
     */
    @Test
    void testSaveAsTemplateNonTeacherThrows() {
        TaskParamsRequest req = TaskParamsRequest.builder().cefrLevel(CefrLevel.B1).taskType(TaskType.MCQ).build();
        assertThrows(ForbiddenException.class, () -> taskService.saveAsTemplate(req, studentUser));
    }

    /**
     * @brief Verifies unit test scenario: create custom task success.
     */
    @Test
    void testCreateCustomTaskSuccess() {
        CustomQuestionRequest q1 = CustomQuestionRequest.builder()
            .questionOrder(1)
            .questionText("Choose the correct passive form.")
            .correctAnswer("is spoken")
            .options(List.of("is spoken", "speaks", "was spoken"))
            .difficulty(2)
            .points(25)
            .grammarRule("Passive Voice")
            .build();

        CustomQuestionRequest q2 = CustomQuestionRequest.builder()
            .questionOrder(2)
            .questionText("Rewrite: They built the bridge.")
            .correctAnswer("The bridge was built.")
            .difficulty(3)
            .points(25)
            .build();

        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Passive Voice")
            .domain("Academic")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.MEDIUM)
            .totalPoints(50)
            .content("Complete the sentences.")
            .questions(List.of(q1, q2))
            .isTemplate(false)
            .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse res = taskService.createCustomTask(req, teacherUser);
        assertNotNull(res);
        assertEquals(2, res.getQuestions().size());
        assertEquals("is spoken", res.getQuestions().get(0).getCorrectAnswer());
        assertEquals("The bridge was built.", res.getQuestions().get(1).getCorrectAnswer());
        assertEquals(50, res.getTotalPoints());
        assertEquals(25, res.getQuestions().get(0).getPoints());
        assertEquals(25, res.getQuestions().get(1).getPoints());
        verify(taskRepository).save(any(Task.class));
    }

    /**
     * @brief Verifies unit test scenario: create custom task with default fallbacks.
     */
    @Test
    void testCreateCustomTaskDefaults() {
        CustomQuestionRequest q = CustomQuestionRequest.builder()
            .questionOrder(0)
            .questionText("Question with defaults")
            .correctAnswer("Answer")
            .difficulty(0)
            .grammarRule("")
            .build();

        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.A1)
            .grammarTopic("")
            .domain("")
            .taskType(TaskType.REWRITE)
            .difficulty(null)
            .content(null)
            .questions(List.of(q))
            .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse res = taskService.createCustomTask(req, teacherUser);
        assertNotNull(res);
        assertEquals("Custom Topic", res.getGrammarTopic());
        assertEquals("General", res.getDomain());
        assertEquals(DifficultyLevel.MEDIUM, res.getDifficulty());
        assertEquals(10, res.getTotalPoints());
        assertEquals(10, res.getQuestions().get(0).getPoints());
    }

    /**
     * @brief Verifies that custom vocabulary and rules are wrapped and saved into task answerKey for non-essay tasks.
     */
    @Test
    void testCreateCustomTaskWithCustomVocabularyAndRule() {
        CustomQuestionRequest q = CustomQuestionRequest.builder()
            .questionOrder(1)
            .questionText("Choose the correct option.")
            .options(List.of("resilient", "fragile"))
            .correctAnswer("resilient")
            .points(15)
            .build();

        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Adjectives & Collocations")
            .taskType(TaskType.MCQ)
            .customVocabulary("resilient, endurance, fortitude")
            .customRule("Compound Adjectives")
            .questions(List.of(q))
            .build();

        final java.util.concurrent.atomic.AtomicReference<Task> savedRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            savedRef.set(t);
            return t;
        });

        TaskResponse res = taskService.createCustomTask(req, teacherUser);
        assertNotNull(res);
        assertNotNull(savedRef.get());
        assertTrue(savedRef.get().getAnswerKey().contains("resilient, endurance, fortitude"));
        assertTrue(savedRef.get().getAnswerKey().contains("Compound Adjectives"));
        assertTrue(savedRef.get().getAnswerKey().contains("answers"));
    }

    /**
     * @brief Verifies unit test scenario: create custom task with group assignment.
     */
    @Test
    void testCreateCustomTaskWithGroupAssignment() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).name("Cohort 1").teacher(teacherUser).build();
        GroupStudent gs = GroupStudent.builder().group(group).student(studentUser).isActive(true).build();

        CustomQuestionRequest q = CustomQuestionRequest.builder()
            .questionOrder(1)
            .questionText("Fill in the blank.")
            .correctAnswer("had gone")
            .build();

        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.GAP_FILL)
            .questions(List.of(q))
            .groupIds(List.of(groupId))
            .dueDate(LocalDateTime.now().plusDays(3))
            .maxAttempts(2)
            .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(taskRepository.findById(any(UUID.class))).thenAnswer(inv -> {
            return Optional.of(Task.builder().id(inv.getArgument(0)).type(TaskType.GAP_FILL).cefrLevel(CefrLevel.B2).build());
        });
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of(gs));

        TaskResponse res = taskService.createCustomTask(req, teacherUser);
        assertNotNull(res);
        verify(taskAssignmentRepository).save(any(TaskAssignment.class));
    }

    /**
     * @brief Verifies unit test scenario: create custom task non teacher throws.
     */
    @Test
    void testCreateCustomTaskNonTeacherThrows() {
        CreateCustomTaskRequest req = CreateCustomTaskRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .taskType(TaskType.MCQ)
            .questions(List.of(CustomQuestionRequest.builder().questionText("Q").correctAnswer("A").build()))
            .build();

        assertThrows(ForbiddenException.class, () -> taskService.createCustomTask(req, studentUser));
    }

    /**
     * @brief Verifies QuestionResponse fromEntityWithoutAnswer.
     */
    @Test
    void testQuestionResponseFromEntityWithoutAnswer() {
        com.linguaoptima.api.domain.TaskQuestion tq = com.linguaoptima.api.domain.TaskQuestion.builder()
            .id(UUID.randomUUID())
            .questionOrder(1)
            .questionText("Test text")
            .correctAnswer("Secret Answer")
            .optionsJson("[\"A\", \"B\"]")
            .difficulty(2)
            .grammarRule("Test Rule")
            .build();

        QuestionResponse qr = QuestionResponse.fromEntityWithoutAnswer(tq);
        assertNotNull(qr);
        assertNull(qr.getCorrectAnswer());
        assertEquals("Test text", qr.getQuestionText());
        assertEquals(2, qr.getOptions().size());

        assertNull(QuestionResponse.fromEntityWithoutAnswer(null));
        assertNull(QuestionResponse.fromEntity(null));
    }

    /**
     * @brief Verifies unit test scenario: assign task teacher success.
     */
    @Test
    void testAssignTaskTeacherSuccess() {
        UUID taskId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Task task = Task.builder().id(taskId).type(TaskType.MCQ).cefrLevel(CefrLevel.B1).build();
        Group group = Group.builder().id(groupId).name("Group Alpha").teacher(teacherUser).build();
        GroupStudent gs = GroupStudent.builder().group(group).student(studentUser).isActive(true).build();

        AssignTaskRequest req = AssignTaskRequest.builder()
            .groupIds(List.of(groupId))
            .dueDate(LocalDateTime.now().plusDays(7))
            .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of(gs));

        taskService.assignTask(taskId, req, teacherUser);

        verify(taskAssignmentRepository).save(argThat(a -> a.getStudent().getId().equals(studentUser.getId())));
        verify(notificationService).send(eq(studentUser), anyString(), eq(NotificationType.TASK));
    }

    /**
     * @brief Verifies unit test scenario: assign task not group teacher throws.
     */
    @Test
    void testAssignTaskNotGroupTeacherThrows() {
        UUID taskId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        User otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        Task task = Task.builder().id(taskId).build();
        Group group = Group.builder().id(groupId).teacher(otherTeacher).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        AssignTaskRequest req = AssignTaskRequest.builder().groupIds(List.of(groupId)).build();
        assertThrows(ForbiddenException.class, () -> taskService.assignTask(taskId, req, teacherUser));
    }

    /**
     * @brief Verifies unit test scenario: get tasks and get by id.
     */
    @Test
    void testGetTasksAndGetById() {
        Task t = Task.builder().id(UUID.randomUUID()).type(TaskType.MCQ).cefrLevel(CefrLevel.B1).content("Test").build();
        when(taskRepository.findAllAccessibleForUser(studentUser.getId())).thenReturn(List.of(t));
        when(taskRepository.findById(t.getId())).thenReturn(Optional.of(t));

        assertEquals(1, taskService.getTasksForUser(studentUser).size());
        assertEquals("Test", taskService.getTaskById(t.getId()).getContent());

        UUID unknown = UUID.randomUUID();
        when(taskRepository.findById(unknown)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(unknown));
    }

    /**
     * @brief Verifies unit test scenario: assign task task not found throws.
     */
    @Test
    void testAssignTaskTaskNotFoundThrows() {
        UUID unknown = UUID.randomUUID();
        when(taskRepository.findById(unknown)).thenReturn(Optional.empty());
        AssignTaskRequest req = AssignTaskRequest.builder().groupIds(List.of(UUID.randomUUID())).build();

        assertThrows(ResourceNotFoundException.class, () -> taskService.assignTask(unknown, req, teacherUser));
    }

    /**
     * @brief Verifies unit test scenario: assign task group not found throws.
     */
    @Test
    void testAssignTaskGroupNotFoundThrows() {
        UUID taskId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Task task = Task.builder().id(taskId).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        AssignTaskRequest req = AssignTaskRequest.builder().groupIds(List.of(groupId)).build();
        assertThrows(ResourceNotFoundException.class, () -> taskService.assignTask(taskId, req, teacherUser));
    }

    /**
     * @brief Verifies unit test scenario: generate task with malformed json fallback.
     */
    @Test
    void testGenerateTaskWithMalformedJsonFallback() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Articles")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.EASY)
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("not a json string");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertEquals("Practice exercise for " + CefrLevel.B1, res.getContent());
    }

    /**
     * @brief Verifies unit test scenario: default parameter fallbacks and non-teacher assignTask restriction.
     */
    @Test
    void testDefaultParamsAndNonTeacherAssignThrows() {
        TaskParamsRequest minimalReq = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .taskType(TaskType.GAP_FILL)
            .grammarTopic(null)
            .domain(null)
            .difficulty(null)
            .numberOfQuestions(0)
            .build();

        String jsonNoOptions = """
            {
              "content": "Fill in the blanks",
              "questions": [
                { "text": "She ___ to school.", "correctAnswer": "goes" },
                { "text": "He ___ home." }
              ],
              "answerKey": [
                { "questionOrder": 2, "correctOption": "   " }
              ]
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), any())).thenReturn(jsonNoOptions);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse generated = taskService.generateTask(minimalReq, teacherUser);
        assertEquals(DifficultyLevel.MEDIUM, generated.getDifficulty());

        TaskParamsRequest withTopicAndDomain = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.ESSAY)
            .grammarTopic("Passive Voice")
            .domain("Academic")
            .difficulty(DifficultyLevel.HARD)
            .numberOfQuestions(4)
            .build();

        User adminUser = User.builder().id(UUID.randomUUID()).email("admin@lingua.com").fullName("Admin").role(Role.ADMIN).build();
        assertNotNull(taskService.previewTask(withTopicAndDomain, studentUser));
        assertNotNull(taskService.saveAsTemplate(withTopicAndDomain, teacherUser));
        assertNotNull(taskService.saveAsTemplate(withTopicAndDomain, adminUser));

        UUID taskId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Task task = Task.builder().id(taskId).type(TaskType.MCQ).cefrLevel(CefrLevel.B1).build();
        Group adminGroup = Group.builder().id(groupId).name("Admin Group").teacher(adminUser).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(adminGroup));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of());
        AssignTaskRequest adminAssignReq = AssignTaskRequest.builder().groupIds(List.of(groupId)).build();
        assertDoesNotThrow(() -> taskService.assignTask(taskId, adminAssignReq, adminUser));

        AssignTaskRequest assignReq = AssignTaskRequest.builder().groupIds(List.of(UUID.randomUUID())).build();
        assertThrows(ForbiddenException.class, () -> taskService.assignTask(UUID.randomUUID(), assignReq, studentUser));
    }

    /**
     * @brief Verifies unit test scenario: markdown code block and prompt leak sanitization for MCQ task.
     */
    @Test
    void testGenerateTaskWithMarkdownCodeBlockAndPromptLeak() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Reported Speech")
            .domain("Workplace")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.MEDIUM)
            .build();

        String wrappedJsonWithLeak = """
            ```json
            {
              "content": "Generate an English grammar exercise based on the following parameters: - CEFR Level: B1 - Grammar Topic: Reported Speech - Return ONLY a valid JSON",
              "questions": [
                { "text": "He said he was tired.", "options": ["yes", "no"] }
              ]
            }
            ```
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(wrappedJsonWithLeak);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertFalse(res.getContent().contains("Generate an English"));
        assertTrue(res.getContent().contains("Complete the following exercises focusing on Reported Speech."));
    }

    /**
     * @brief Verifies unit test scenario: essay task prompt leak sanitization and topic fallback.
     */
    @Test
    void testGenerateEssayTaskWithPromptLeakFallback() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .grammarTopic("Subjunctive Mood")
            .domain("Higher Education")
            .taskType(TaskType.ESSAY)
            .build();

        String wrappedJsonWithPromptLeak = """
            ```
            {
              "content": "Overall instructions or context passage: CRITICAL: Do NOT echo system parameters",
              "questions": []
            }
            ```
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn(wrappedJsonWithPromptLeak);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.saveAsTemplate(req, teacherUser);
        assertNotNull(res);
        assertFalse(res.getContent().contains("CRITICAL"));
        assertTrue(res.getContent().contains("Write an essay discussing Subjunctive Mood"));
    }

    /**
     * @brief Verifies unit test scenario: blank content sanitized to contextual default.
     */
    @Test
    void testGenerateTaskWithBlankContentFallback() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .taskType(TaskType.GAP_FILL)
            .build();

        String jsonWithBlankContent = """
            {
              "content": "   ",
              "questions": []
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(jsonWithBlankContent);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("Complete the following exercises"));
    }

    /**
     * @brief Verifies unit test scenario: null AI response falls back safely.
     */
    @Test
    void testGenerateTaskWithNullAiResponse() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .taskType(TaskType.MCQ)
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("Complete the following exercises"));
    }

    /**
     * @brief Verifies unit test scenario: essay task with null grammar topic and domain uses default context strings.
     */
    @Test
    void testGenerateEssayTaskWithNullTopicAndDomain() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .taskType(TaskType.ESSAY)
            .grammarTopic(null)
            .domain(null)
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("{\"content\":\"\"}");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("General Topic"));
        assertTrue(res.getContent().contains("Daily Life"));
    }

    /**
     * @brief Verifies prompt leak detection sanitizes AI content to default instructions.
     */
    @Test
    void testGenerateTaskWithPromptLeakContentFallback() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.MCQ)
            .grammarTopic("Conditionals")
            .build();

        String leakedJson = "{\"content\": \"System parameters and instructions: generate an english test\", \"questions\": []}";
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(leakedJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("Conditionals"));
        assertTrue(res.getContent().contains("Complete the following exercises focusing on"));
    }

    /**
     * @brief Verifies essay fallback incorporates custom topic and domain when present.
     */
    @Test
    void testGenerateEssayTaskWithProvidedTopicAndDomain() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.ESSAY)
            .grammarTopic("Artificial Intelligence")
            .domain("Higher Education")
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("{\"content\":\"\"}");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("Artificial Intelligence"));
        assertTrue(res.getContent().contains("Higher Education"));
    }

    /**
     * @brief Verifies essay fallback with blank topic and domain uses default context strings.
     */
    @Test
    void testGenerateEssayTaskWithBlankTopicAndDomain() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .taskType(TaskType.ESSAY)
            .grammarTopic("   ")
            .domain("   ")
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("{\"content\":\"\"}");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("General Topic"));
        assertTrue(res.getContent().contains("Daily Life"));
    }

    /**
     * @brief Verifies non-essay fallback with blank topic uses default English grammar string.
     */
    @Test
    void testGenerateNonEssayTaskWithBlankTopic() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .taskType(TaskType.MCQ)
            .grammarTopic("   ")
            .build();

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn("{\"content\":\"\"}");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertTrue(res.getContent().contains("English grammar"));
    }

    /**
     * @brief Verifies that essay content containing gap-fill exercise markers is replaced with clean essay instructions.
     */
    @Test
    void testGenerateEssayTaskWithInappropriateExerciseMarkersSanitized() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .taskType(TaskType.ESSAY)
            .grammarTopic("Cleft sentences")
            .domain("Technology")
            .build();

        String rawJson = "{\"content\":\"For each numbered blank, choose the correct cleft construction.\",\"questions\":[]}";
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(rawJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertFalse(res.getContent().contains("numbered blank"));
        assertTrue(res.getContent().contains("Write an essay discussing Cleft sentences"));
    }

    /**
     * @brief Verifies valid clean essay content without markers is preserved.
     */
    @Test
    void testGenerateEssayTaskWithValidCleanContentPreserved() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.C1)
            .taskType(TaskType.ESSAY)
            .grammarTopic("Cleft sentences")
            .domain("Technology")
            .build();

        String cleanContent = "Evaluate the socioeconomic impacts of autonomous AI systems. Discuss both opportunities and risks.";
        String rawJson = "{\"content\":\"" + cleanContent + "\",\"questions\":[]}";
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(rawJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        assertEquals(cleanContent, res.getContent());
    }

    /**
     * @brief Verifies topics catalog retrieval containing level topics, mixed topics, and domains.
     */
    @Test
    void testGetTopicsCatalog() {
        com.linguaoptima.api.dto.response.TopicsCatalogResponse catalog = taskService.getTopicsCatalog();
        assertNotNull(catalog);
        assertNotNull(catalog.getTopicsByLevel());
        assertEquals(6, catalog.getTopicsByLevel().size());
        assertNotNull(catalog.getMixedTopicsByLevel());
        assertEquals(6, catalog.getMixedTopicsByLevel().size());
        assertNotNull(catalog.getCrossLevelTopics());
        assertFalse(catalog.getCrossLevelTopics().isEmpty());
        assertNotNull(catalog.getDomains());
        assertFalse(catalog.getDomains().isEmpty());
    }

    @Test
    void testGenerateTaskWithResolvedCurriculumContext() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Mixed Conditionals")
            .domain("Science")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.HARD)
            .numberOfQuestions(3)
            .customRule("Custom Rule")
            .customVocabulary("vocab1, vocab2")
            .build();

        when(curriculumStorageService.resolvePromptCurriculumContext(
            eq(CefrLevel.B2), eq("Mixed Conditionals"), eq("Custom Rule"), eq("vocab1, vocab2")
        )).thenReturn(new String[] { "Injected Rule Content", "Injected Vocabulary List" });

        String rawJson = "{\"content\":\"Test curriculum content\",\"questions\":[]}";
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(rawJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        verify(curriculumStorageService).resolvePromptCurriculumContext(
            eq(CefrLevel.B2), eq("Mixed Conditionals"), eq("Custom Rule"), eq("vocab1, vocab2")
        );
    }

    @Test
    void testGenerateTaskWithEcoMode() {
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Mixed Conditionals")
            .domain("Science")
            .taskType(TaskType.MCQ)
            .difficulty(DifficultyLevel.HARD)
            .numberOfQuestions(3)
            .ecoMode(true)
            .build();

        String rawJson = "{\"content\":\"Eco mode test content\",\"questions\":[]}";
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(rawJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        // Verify that curriculum context was NEVER resolved because Eco Mode was active
        verify(curriculumStorageService, never()).resolvePromptCurriculumContext(any(), any(), any(), any());
    }

    @Test
    void testAssignTaskMaxAttemptsAndEnrichTaskForUser() {
        UUID taskId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Task task = Task.builder()
            .id(taskId)
            .type(TaskType.MCQ)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Conditionals")
            .build();
        Group group = Group.builder()
            .id(groupId)
            .name("B1 Group")
            .teacher(teacherUser)
            .build();
        GroupStudent gs = GroupStudent.builder()
            .group(group)
            .student(studentUser)
            .isActive(true)
            .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of(gs));

        // 1. Negative maxAttempts defaults to 1
        AssignTaskRequest negReq = AssignTaskRequest.builder()
            .groupIds(List.of(groupId))
            .maxAttempts(-5)
            .build();
        taskService.assignTask(taskId, negReq, teacherUser);

        // 2. Explicit 0 (unlimited) maxAttempts
        AssignTaskRequest unlimReq = AssignTaskRequest.builder()
            .groupIds(List.of(groupId))
            .maxAttempts(0)
            .build();
        taskService.assignTask(taskId, unlimReq, teacherUser);

        // 3. Enrich task when assigned by teacher, 1 attempt used out of 1, with submissions
        UUID assignId = UUID.randomUUID();
        UUID latestSubId = UUID.randomUUID();
        TaskAssignment teacherAssign = TaskAssignment.builder()
            .id(assignId)
            .task(task)
            .student(studentUser)
            .assignedBy(teacherUser)
            .dueDate(LocalDateTime.now().plusDays(2))
            .status(AssignmentStatus.SUBMITTED)
            .maxAttempts(1)
            .attemptsUsed(0) // legacy submitted row with 0 attemptsUsed -> coerced to 1
            .build();

        com.linguaoptima.api.domain.Submission subOld = com.linguaoptima.api.domain.Submission.builder()
            .id(UUID.randomUUID())
            .submittedAt(LocalDateTime.now().minusHours(2))
            .build();
        com.linguaoptima.api.domain.Submission subNew = com.linguaoptima.api.domain.Submission.builder()
            .id(latestSubId)
            .submittedAt(LocalDateTime.now())
            .build();

        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentUser.getId(), taskId))
            .thenReturn(Optional.of(teacherAssign));
        when(submissionRepository.findByAssignmentId(assignId))
            .thenReturn(List.of(subOld, subNew));

        TaskResponse enriched = taskService.getTaskById(taskId, studentUser);
        assertEquals(assignId, enriched.getAssignmentId());
        assertEquals("Teacher Alice", enriched.getAssignedByName());
        assertEquals("SUBMITTED", enriched.getAssignmentStatus());
        assertEquals(1, enriched.getMaxAttempts());
        assertEquals(1, enriched.getAttemptsUsed());
        assertFalse(enriched.getCanSubmit());
        assertEquals(latestSubId, enriched.getLatestSubmissionId());

        // 4. Enrich task when assigned by self, null status, null maxAttempts, GRADED status, and unlimited attempts
        TaskAssignment selfAssign = TaskAssignment.builder()
            .id(assignId)
            .task(task)
            .student(studentUser)
            .assignedBy(studentUser)
            .status(AssignmentStatus.GRADED)
            .maxAttempts(null)
            .attemptsUsed(0)
            .build();
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentUser.getId(), taskId))
            .thenReturn(Optional.of(selfAssign));
        TaskResponse enrichedGraded = taskService.getTaskById(taskId, studentUser);
        assertNull(enrichedGraded.getAssignedByName());
        assertEquals(1, enrichedGraded.getAttemptsUsed());
        assertFalse(enrichedGraded.getCanSubmit());

        // 5. Enrich task with null assignedBy, null status, maxAttempts = 0 (unlimited), and attemptsUsed < maxAttempts
        TaskAssignment unlimitedAssign = TaskAssignment.builder()
            .id(assignId)
            .task(task)
            .student(studentUser)
            .assignedBy(null)
            .status(null)
            .maxAttempts(0)
            .attemptsUsed(2)
            .build();
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentUser.getId(), taskId))
            .thenReturn(Optional.of(unlimitedAssign));
        TaskResponse enrichedUnlimited = taskService.getTaskById(taskId, studentUser);
        assertEquals("PENDING", enrichedUnlimited.getAssignmentStatus());
        assertTrue(enrichedUnlimited.getCanSubmit());

        TaskAssignment pendingSingle = TaskAssignment.builder()
            .id(assignId)
            .task(task)
            .student(studentUser)
            .assignedBy(teacherUser)
            .status(AssignmentStatus.PENDING)
            .maxAttempts(1)
            .attemptsUsed(0)
            .build();
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentUser.getId(), taskId))
            .thenReturn(Optional.of(pendingSingle));
        TaskResponse enrichedPending = taskService.getTaskById(taskId, studentUser);
        assertTrue(enrichedPending.getCanSubmit());
    }

    /**
     * @brief Verifies unit test scenario: REWRITE and OPEN_BRACKETS generation enforces empty options array.
     */
    @Test
    void testGenerateTaskRewriteAndOpenBracketsEnforcesEmptyOptions() {
        String jsonWithOptions = """
            {
              "content": "Exercise content",
              "questions": [
                {
                  "text": "Rewrite sentence",
                  "options": ["A", "B", "C"],
                  "correctAnswer": "Rewritten sentence"
                }
              ],
              "answerKey": []
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(jsonWithOptions);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskParamsRequest rewriteReq = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Conditionals")
            .taskType(TaskType.REWRITE)
            .build();

        TaskResponse rewriteRes = taskService.generateTask(rewriteReq, studentUser);
        assertNotNull(rewriteRes);
        assertEquals(TaskType.REWRITE, rewriteRes.getType());
        assertEquals(1, rewriteRes.getQuestions().size());
        assertEquals(0, rewriteRes.getQuestions().get(0).getOptions().size());

        TaskParamsRequest bracketsReq = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Past Simple vs Present Perfect")
            .taskType(TaskType.OPEN_BRACKETS)
            .build();

        TaskResponse bracketsRes = taskService.generateTask(bracketsReq, studentUser);
        assertNotNull(bracketsRes);
        assertEquals(TaskType.OPEN_BRACKETS, bracketsRes.getType());
        assertEquals(1, bracketsRes.getQuestions().size());
        assertEquals(0, bracketsRes.getQuestions().get(0).getOptions().size());
    }

    /**
     * @brief Verifies unit test scenario: educator generates a task directly with multiple cohort group assignments.
     */
    @Test
    void testGenerateTaskMultiGroupAssignment() {
        UUID group1Id = UUID.randomUUID();
        UUID group2Id = UUID.randomUUID();
        Group group1 = Group.builder().id(group1Id).name("Cohort A").teacher(teacherUser).build();
        Group group2 = Group.builder().id(group2Id).name("Cohort B").teacher(teacherUser).build();

        User student1 = User.builder().id(UUID.randomUUID()).email("s1@lingua.com").fullName("Student 1").role(Role.STUDENT).build();
        User student2 = User.builder().id(UUID.randomUUID()).email("s2@lingua.com").fullName("Student 2").role(Role.STUDENT).build();

        GroupStudent gs1 = GroupStudent.builder().group(group1).student(student1).isActive(true).build();
        GroupStudent gs2 = GroupStudent.builder().group(group2).student(student2).isActive(true).build();

        when(groupRepository.findByTeacher(teacherUser)).thenReturn(List.of(group1, group2));
        when(groupRepository.findById(group1Id)).thenReturn(Optional.of(group1));
        when(groupRepository.findById(group2Id)).thenReturn(Optional.of(group2));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group1Id)).thenReturn(List.of(gs1));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group2Id)).thenReturn(List.of(gs2));

        PricingProperties.TierConfig tierConfig = new PricingProperties.TierConfig();
        tierConfig.setMaxGroups(5);
        when(pricingProperties.getTierConfig(any())).thenReturn(tierConfig);

        String json = """
            {
              "content": "Exercise content",
              "questions": [
                {
                  "text": "Question 1",
                  "options": ["A", "B"],
                  "correctAnswer": "A"
                }
              ],
              "answerKey": []
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn(json);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(taskRepository.findById(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(Task.builder().id(id).type(TaskType.MCQ).cefrLevel(CefrLevel.B1).build());
        });

        LocalDateTime due = LocalDateTime.now().plusDays(7);
        TaskParamsRequest req = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Conditionals")
            .taskType(TaskType.MCQ)
            .groupIds(List.of(group1Id, group2Id))
            .dueDate(due)
            .maxAttempts(2)
            .build();

        TaskResponse res = taskService.generateTask(req, teacherUser);
        assertNotNull(res);
        verify(taskAssignmentRepository, times(2)).save(any(TaskAssignment.class));
        verify(notificationService, times(2)).send(any(User.class), anyString(), eq(NotificationType.TASK));
    }

    /**
     * @brief Verifies unit test scenario: retrieving tasks for a group with aggregated completion and scores.
     */
    @Test
    void testGetTasksForGroupAllScenarios() {
        UUID groupId = UUID.randomUUID();

        // 1. Non-teacher role throws
        assertThrows(ForbiddenException.class, () -> taskService.getTasksForGroup(groupId, studentUser));

        // 2. Non-owner teacher throws
        User otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        Group otherGroup = Group.builder().id(groupId).name("Other").teacher(otherTeacher).build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(otherGroup));
        assertThrows(ForbiddenException.class, () -> taskService.getTasksForGroup(groupId, teacherUser));

        // 3. Empty group returns empty list
        Group group = Group.builder().id(groupId).name("Group 1").teacher(teacherUser).build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of());

        List<GroupTaskResponse> emptyRes = taskService.getTasksForGroup(groupId, teacherUser);
        assertTrue(emptyRes.isEmpty());

        // 4. Group with students and assigned tasks
        User student1 = User.builder().id(UUID.randomUUID()).fullName("Student 1").build();
        GroupStudent gs1 = GroupStudent.builder().group(group).student(student1).isActive(true).build();
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of(gs1));

        Task task = Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MCQ)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Passive Voice")
            .domain("Science")
            .totalPoints(100)
            .createdAt(LocalDateTime.now())
            .build();

        TaskAssignment assign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student1)
            .assignedBy(teacherUser)
            .dueDate(LocalDateTime.now().plusDays(3))
            .status(AssignmentStatus.GRADED)
            .maxAttempts(1)
            .build();

        when(taskAssignmentRepository.findByStudentIdsWithTaskAndStudent(List.of(student1.getId())))
            .thenReturn(List.of(assign));

        com.linguaoptima.api.domain.Submission sub = com.linguaoptima.api.domain.Submission.builder()
            .id(UUID.randomUUID())
            .assignment(assign)
            .student(student1)
            .studentText("Answer")
            .aiScore(88.0)
            .build();

        when(submissionRepository.findByAssignmentIdIn(List.of(assign.getId()))).thenReturn(List.of(sub));

        List<GroupTaskResponse> res = taskService.getTasksForGroup(groupId, teacherUser);
        assertEquals(1, res.size());
        GroupTaskResponse gtr = res.get(0);
        assertEquals(task.getId(), gtr.getTaskId());
        assertEquals("Passive Voice", gtr.getGrammarTopic());
        assertEquals(1, gtr.getAssignedStudentCount());
        assertEquals(1, gtr.getCompletedStudentCount());
        assertEquals(88.0, gtr.getAvgScore());
    }

    /**
     * @brief Verifies unit test scenario: transferring or copying tasks between cohorts.
     */
    @Test
    void testTransferTaskAllScenarios() {
        UUID sourceGroupId = UUID.randomUUID();
        UUID targetGroupId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        TransferTaskRequest req = TransferTaskRequest.builder()
            .targetGroupId(targetGroupId)
            .keepInSource(false)
            .build();

        // 1. Non-teacher throws
        assertThrows(ForbiddenException.class, () -> taskService.transferTask(sourceGroupId, taskId, req, studentUser));

        // 2. Same source and target group throws
        TransferTaskRequest sameReq = TransferTaskRequest.builder().targetGroupId(sourceGroupId).build();
        assertThrows(IllegalArgumentException.class, () -> taskService.transferTask(sourceGroupId, taskId, sameReq, teacherUser));

        // 3. Not owner of source group
        User otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        Group sourceGroupOther = Group.builder().id(sourceGroupId).name("Source Other").teacher(otherTeacher).build();
        when(groupRepository.findById(sourceGroupId)).thenReturn(Optional.of(sourceGroupOther));
        assertThrows(ForbiddenException.class, () -> taskService.transferTask(sourceGroupId, taskId, req, teacherUser));

        // 4. Not owner of target group
        Group sourceGroup = Group.builder().id(sourceGroupId).name("Source").teacher(teacherUser).build();
        Group targetGroupOther = Group.builder().id(targetGroupId).name("Target Other").teacher(otherTeacher).build();
        when(groupRepository.findById(sourceGroupId)).thenReturn(Optional.of(sourceGroup));
        when(groupRepository.findById(targetGroupId)).thenReturn(Optional.of(targetGroupOther));
        assertThrows(ForbiddenException.class, () -> taskService.transferTask(sourceGroupId, taskId, req, teacherUser));

        // 5. Target group tier limit exceeded
        Group targetGroup = Group.builder().id(targetGroupId).name("Target").teacher(teacherUser).build();
        when(groupRepository.findById(targetGroupId)).thenReturn(Optional.of(targetGroup));
        when(groupRepository.findByTeacher(teacherUser)).thenReturn(List.of(sourceGroup, targetGroup));

        PricingProperties.TierConfig tierConfig = new PricingProperties.TierConfig();
        tierConfig.setMaxGroups(1); // Free plan with 1 active group limit
        when(pricingProperties.getTierConfig(any())).thenReturn(tierConfig);

        assertThrows(QuotaExceededException.class, () -> taskService.transferTask(sourceGroupId, taskId, req, teacherUser));

        // 6. Successful MOVE transfer (keepInSource = false)
        tierConfig.setMaxGroups(5);
        Task task = Task.builder()
            .id(taskId)
            .type(TaskType.MCQ)
            .cefrLevel(CefrLevel.B1)
            .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        User studentSource = User.builder().id(UUID.randomUUID()).fullName("Student Source").build();
        GroupStudent gsSource = GroupStudent.builder().group(sourceGroup).student(studentSource).isActive(true).build();
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(sourceGroupId)).thenReturn(List.of(gsSource));

        User studentTarget = User.builder().id(UUID.randomUUID()).fullName("Student Target").build();
        GroupStudent gsTarget = GroupStudent.builder().group(targetGroup).student(studentTarget).isActive(true).build();
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(targetGroupId)).thenReturn(List.of(gsTarget));

        TaskAssignment sourceAssignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(studentSource)
            .assignedBy(teacherUser)
            .status(AssignmentStatus.PENDING)
            .attemptsUsed(0)
            .build();
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentSource.getId(), taskId))
            .thenReturn(Optional.of(sourceAssignment));
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentTarget.getId(), taskId))
            .thenReturn(Optional.empty());

        taskService.transferTask(sourceGroupId, taskId, req, teacherUser);

        // Target student received assignment
        verify(taskAssignmentRepository).save(any(TaskAssignment.class));
        verify(notificationService).send(eq(studentTarget), anyString(), eq(NotificationType.TASK));
        // Source student unstarted pending assignment deleted
        verify(taskAssignmentRepository).delete(sourceAssignment);

        // 7. Successful COPY transfer (keepInSource = true)
        TransferTaskRequest copyReq = TransferTaskRequest.builder()
            .targetGroupId(targetGroupId)
            .keepInSource(true)
            .build();
        clearInvocations(taskAssignmentRepository);

        taskService.transferTask(sourceGroupId, taskId, copyReq, teacherUser);
        verify(taskAssignmentRepository).save(any(TaskAssignment.class));
        verify(taskAssignmentRepository, never()).delete(any(TaskAssignment.class));
    }

    /**
     * @brief Verifies device generation takeover, status queries, multi-group assignment error paths, and transfer error validations.
     */
    @Test
    void testTaskService_AdditionalCoverage() {
        UUID taskId = UUID.randomUUID();
        UUID sourceGroupId = UUID.randomUUID();
        UUID targetGroupId = UUID.randomUUID();

        Group sourceGroup = Group.builder().id(sourceGroupId).name("Source Group").teacher(teacherUser).build();
        Group targetGroup = Group.builder().id(targetGroupId).name("Target Group").teacher(teacherUser).build();
        Task task = Task.builder().id(taskId).type(TaskType.MCQ).cefrLevel(CefrLevel.B1).content("Task Content").build();

        User studentSource = User.builder().id(UUID.randomUUID()).email("source@test.com").role(Role.STUDENT).build();
        User studentTarget = User.builder().id(UUID.randomUUID()).email("target@test.com").role(Role.STUDENT).build();

        // 1. Generation protection delegation
        when(generationProtectionService.getStatus(teacherUser.getId(), "dev-test"))
            .thenReturn(com.linguaoptima.api.dto.response.GenerationStatusResponse.builder().isGenerating(true).build());
        taskService.takeoverGeneration(teacherUser, "dev-test");
        verify(generationProtectionService).takeoverGeneration(teacherUser.getId(), "dev-test");

        var status = taskService.getGenerationStatus(teacherUser, "dev-test");
        assertTrue(status.isGenerating());

        // Null user fallback
        var nullStatus = taskService.getGenerationStatus(null, "dev-test");
        assertFalse(nullStatus.isGenerating());
        taskService.takeoverGeneration(null, "dev-test");

        // 2. assignTask with non-existent group
        UUID missingGroupId = UUID.randomUUID();
        when(groupRepository.findById(missingGroupId)).thenReturn(Optional.empty());
        AssignTaskRequest assignReq = AssignTaskRequest.builder().groupIds(List.of(missingGroupId)).build();
        assertThrows(ResourceNotFoundException.class, () -> taskService.assignTask(taskId, assignReq, teacherUser));

        // 3. transferTask non-existent source, target, task
        UUID nonExistentSource = UUID.randomUUID();
        UUID nonExistentTarget = UUID.randomUUID();
        when(groupRepository.findById(nonExistentSource)).thenReturn(Optional.empty());
        TransferTaskRequest req = TransferTaskRequest.builder().targetGroupId(nonExistentTarget).build();
        assertThrows(ResourceNotFoundException.class, () -> taskService.transferTask(nonExistentSource, taskId, req, teacherUser));

        when(groupRepository.findById(sourceGroupId)).thenReturn(Optional.of(sourceGroup));
        when(groupRepository.findById(nonExistentTarget)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.transferTask(sourceGroupId, taskId, req, teacherUser));

        when(groupRepository.findById(targetGroupId)).thenReturn(Optional.of(targetGroup));
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        TransferTaskRequest validTargetReq = TransferTaskRequest.builder().targetGroupId(targetGroupId).build();
        assertThrows(ResourceNotFoundException.class, () -> taskService.transferTask(sourceGroupId, taskId, validTargetReq, teacherUser));

        // 4. transferTask target group exceeds tier max groups
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Group g3 = Group.builder().id(UUID.randomUUID()).name("Group 3").teacher(teacherUser).build();
        when(groupRepository.findByTeacher(teacherUser)).thenReturn(List.of(sourceGroup, g3, targetGroup));
        when(usageService.getUserTier(teacherUser)).thenReturn(SubscriptionTier.FREE);
        when(pricingProperties.getTierConfig(SubscriptionTier.FREE)).thenReturn(PricingProperties.TierConfig.builder().maxGroups(1).build());
        assertThrows(QuotaExceededException.class, () -> taskService.transferTask(sourceGroupId, taskId, validTargetReq, teacherUser));

        // 5. assignTask when group exceeds maxGroups
        when(groupRepository.findById(targetGroupId)).thenReturn(Optional.of(targetGroup));
        AssignTaskRequest targetGroupReq = AssignTaskRequest.builder().groupIds(List.of(targetGroupId)).dueDate(LocalDateTime.now().plusDays(1)).build();
        assertThrows(QuotaExceededException.class, () -> taskService.assignTask(taskId, targetGroupReq, teacherUser));

        // 6. transferTask with GRADED submission and overrideScore
        when(pricingProperties.getTierConfig(SubscriptionTier.FREE)).thenReturn(PricingProperties.TierConfig.builder().maxGroups(10).build());
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(sourceGroupId)).thenReturn(List.of(GroupStudent.builder().group(sourceGroup).student(studentSource).build()));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(targetGroupId)).thenReturn(List.of(GroupStudent.builder().group(targetGroup).student(studentTarget).build()));

        TaskAssignment gradedAssignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(studentSource)
            .assignedBy(teacherUser)
            .status(AssignmentStatus.GRADED)
            .build();
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentSource.getId(), taskId)).thenReturn(Optional.of(gradedAssignment));
        when(taskAssignmentRepository.findByStudentIdAndTaskId(studentTarget.getId(), taskId)).thenReturn(Optional.empty());

        com.linguaoptima.api.domain.Submission subWithOverride = com.linguaoptima.api.domain.Submission.builder()
            .id(UUID.randomUUID())
            .assignment(gradedAssignment)
            .student(studentSource)
            .overrideScore(95.0)
            .aiScore(80.0)
            .build();
        when(submissionRepository.findByAssignmentId(gradedAssignment.getId())).thenReturn(List.of(subWithOverride));

        TransferTaskRequest moveReq = TransferTaskRequest.builder().targetGroupId(targetGroupId).keepInSource(false).build();
        taskService.transferTask(sourceGroupId, taskId, moveReq, teacherUser);
        verify(taskAssignmentRepository, atLeastOnce()).save(any(TaskAssignment.class));

        // 7. createCustomTask with points <= 0 fallback
        CreateCustomTaskRequest manualReq = CreateCustomTaskRequest.builder()
            .taskType(TaskType.GAP_FILL)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Tenses")
            .content("Fill in blanks")
            .questions(List.of(
                CustomQuestionRequest.builder().questionText("Q1").correctAnswer("A").points(0).difficulty(1).build()
            ))
            .build();
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        TaskResponse manualResp = taskService.createCustomTask(manualReq, teacherUser);
        assertNotNull(manualResp);
    }

    /**
     * @brief Verifies unit test scenario: generate reading task with questions and total points calculation.
     */
    @Test
    void testGenerateReadingTask() {
        String readingJson = """
            {
              "content": "The transition to renewable energy sources has accelerated globally over the past decade.",
              "questions": [
                {
                  "text": "What has accelerated globally over the past decade?",
                  "options": ["Renewable energy transition", "Coal usage", "Deforestation", "Fossil subsidies"],
                  "correctAnswer": "Renewable energy transition",
                  "points": 10,
                  "difficulty": 2,
                  "grammarRule": "Reading: Multiple Choice"
                },
                {
                  "text": "Renewable energy adoption slowed down recently. (True / False)",
                  "options": ["True", "False"],
                  "correctAnswer": "False",
                  "points": 10,
                  "difficulty": 2,
                  "grammarRule": "Reading: True / False"
                },
                {
                  "text": "Complete the sentence: Modern wind turbines generate...",
                  "options": [],
                  "correctAnswer": "clean electricity",
                  "points": 15,
                  "difficulty": 3,
                  "grammarRule": "Reading: Sentence Completion"
                }
              ],
              "answerKey": []
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn(readingJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskParamsRequest req = TaskParamsRequest.builder()
            .taskType(TaskType.READING)
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Renewable Energy")
            .domain("Science")
            .readingTitle("Green Energy Evolution")
            .readingLength("Medium (250-400 words)")
            .readingParagraphs("3 paragraphs")
            .mcqQuestionsCount(1)
            .mcqPoints(10)
            .trueFalseQuestionsCount(1)
            .trueFalsePoints(10)
            .openQuestionsCount(1)
            .openPoints(15)
            .build();

        TaskResponse res = taskService.generateTask(req, teacherUser);
        assertNotNull(res);
        assertEquals(TaskType.READING, res.getType());
        assertEquals(35, res.getTotalPoints());
        assertEquals(3, res.getQuestions().size());
        assertTrue(res.getContent().contains("transition to renewable energy"));

        // Test default reading content fallback when AI content is empty
        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn("{\"content\":\"\"}");
        TaskResponse emptyRes = taskService.generateTask(req, teacherUser);
        assertNotNull(emptyRes);
        assertTrue(emptyRes.getContent().contains("Read the following passage about Renewable Energy carefully"));

        // Test default reading content with null topic
        TaskParamsRequest reqNullTopic = TaskParamsRequest.builder()
            .taskType(TaskType.READING)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic(null)
            .build();
        TaskResponse emptyNullTopicRes = taskService.generateTask(reqNullTopic, teacherUser);
        assertNotNull(emptyNullTopicRes);
        assertTrue(emptyNullTopicRes.getContent().contains("Contemporary Global Trends"));
    }

    /**
     * @brief Tests generation of SPEAKING task, fallback content, and default question creation.
     */
    @Test
    void testGenerateSpeakingTask() {
        String speakingJson = """
            {
              "title": "Mastering Dental Fricatives",
              "content": "Thirty-three thoughtful thieves thought they thrilled the throne throughout Thursday.",
              "targetSounds": "/θ/ vs /ð/",
              "questions": [
                {
                  "id": 1,
                  "questionOrder": 1,
                  "type": "SPEAKING",
                  "text": "Thirty-three thoughtful thieves thought they thrilled the throne throughout Thursday.",
                  "correctAnswer": "Thirty-three thoughtful thieves thought they thrilled the throne throughout Thursday.",
                  "grammarRule": "Target Pronunciation: /θ/",
                  "difficulty": 2,
                  "points": 100,
                  "options": []
                }
              ],
              "answerKey": []
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn(speakingJson);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskParamsRequest req = TaskParamsRequest.builder()
            .taskType(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B2)
            .grammarTopic("Dental Fricatives")
            .domain("Phonetics")
            .speakingTitle("Mastering Dental Fricatives")
            .targetSounds("/θ/ vs /ð/")
            .speakingLength("SHORT")
            .totalPoints(100)
            .build();

        TaskResponse res = taskService.generateTask(req, teacherUser);
        assertNotNull(res);
        assertEquals(TaskType.SPEAKING, res.getType());
        assertEquals(100, res.getTotalPoints());
        assertEquals(1, res.getQuestions().size());
        assertTrue(res.getContent().contains("thoughtful thieves"));

        // Test fallback question creation when questions array is empty
        String speakingNoQuestionsJson = """
            {
              "title": "Natural Rhythm",
              "content": "Good morning everyone. Today we are going to talk about global trade and economy.",
              "targetSounds": "Connected speech"
            }
            """;
        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn(speakingNoQuestionsJson);
        TaskResponse noQRes = taskService.generateTask(req, teacherUser);
        assertNotNull(noQRes);
        assertEquals(1, noQRes.getQuestions().size());
        assertTrue(noQRes.getContent().contains("Good morning everyone"));

        // Test default speaking content fallback when AI content is empty
        when(aiBrokerService.generateTaskContent(anyString(), eq(teacherUser))).thenReturn("{\"content\":\"\"}");
        TaskResponse emptyRes = taskService.generateTask(req, teacherUser);
        assertNotNull(emptyRes);
        assertTrue(emptyRes.getContent().contains("Read the speech practice passage about Dental Fricatives aloud"));

        // Test default speaking content with null topic
        TaskParamsRequest reqNullTopic = TaskParamsRequest.builder()
            .taskType(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic(null)
            .build();
        TaskResponse emptyNullTopicRes = taskService.generateTask(reqNullTopic, teacherUser);
        assertNotNull(emptyNullTopicRes);
        assertTrue(emptyNullTopicRes.getContent().contains("Pronunciation and Natural Flow"));
    }

    /**
     * @brief Verifies that speaking generation injects pronunciation pack drill words when Eco Mode is off.
     */
    @Test
    void testGenerateSpeakingTask_WithPronunciationPack() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        PronunciationPackResponse pack = PronunciationPackResponse.builder()
            .id("B1_DENTAL_FRICATIVES")
            .title("Dental Fricatives: /θ/ and /ð/")
            .drillWords(List.of("thorough", "threaten", "author"))
            .minimalPairs(List.of(
                com.linguaoptima.api.dto.response.MinimalPairDto.builder()
                    .word1("faith").word2("face").phoneme1("/θ/").phoneme2("/s/").build()
            ))
            .contrastFocus("Sustain friction on end consonants without stopping")
            .tongueTwister("The author thought through a thorough method")
            .build();

        when(pronunciationPackService.resolvePack(eq(CefrLevel.B1), isNull(), eq("/θ/ vs /ð/")))
            .thenReturn(pack);

        String sampleResponse = """
            {
              "title": "Dental Fricatives Drill",
              "content": "The author thought through a thorough method of northern therapy.",
              "questions": [
                {
                  "id": 1,
                  "text": "The author thought through a thorough method of northern therapy.",
                  "correctAnswer": "The author thought through a thorough method of northern therapy.",
                  "points": 100
                }
              ]
            }
            """;

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(aiBrokerService.generateTaskContent(promptCaptor.capture(), eq(studentUser)))
            .thenReturn(sampleResponse);

        TaskParamsRequest req = TaskParamsRequest.builder()
            .taskType(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B1)
            .targetSounds("/θ/ vs /ð/")
            .ecoMode(false)
            .build();

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        verify(pronunciationPackService).resolvePack(eq(CefrLevel.B1), isNull(), eq("/θ/ vs /ð/"));

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("TARGET PHONETICS PACK"));
        assertTrue(prompt.contains("Dental Fricatives: /θ/ and /ð/"));
        assertTrue(prompt.contains("thorough, threaten, author"));
    }

    /**
     * @brief Verifies that speaking generation bypasses phonetics pack injection when Eco Mode is active.
     */
    @Test
    void testGenerateSpeakingTask_WithEcoMode() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        String sampleResponse = """
            {
              "title": "Quick Speech",
              "content": "Good morning class.",
              "questions": [
                {
                  "id": 1,
                  "text": "Good morning class.",
                  "correctAnswer": "Good morning class.",
                  "points": 100
                }
              ]
            }
            """;

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(aiBrokerService.generateTaskContent(promptCaptor.capture(), eq(studentUser)))
            .thenReturn(sampleResponse);

        TaskParamsRequest req = TaskParamsRequest.builder()
            .taskType(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B1)
            .targetSounds("/θ/ vs /ð/")
            .ecoMode(true)
            .build();

        TaskResponse res = taskService.generateTask(req, studentUser);
        assertNotNull(res);
        verify(pronunciationPackService, never()).resolvePack(any(), any(), any());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("ECO MODE ACTIVE"));
        assertFalse(prompt.contains("TARGET PHONETICS PACK"));
    }

    /**
     * @brief Verifies unit test scenario: explainSentence parses AI JSON and returns structured explanation.
     */
    @Test
    void testExplainSentenceSuccess() {
        String aiJson = """
            {
              "originalSentence": "If I had known, I would have told you.",
              "translation": "Если бы я знал, я бы сказал тебе.",
              "grammarBreakdown": "Third Conditional (If + Past Perfect, would + have + V3)",
              "keyVocabulary": ["known — узнал", "told — рассказал"],
              "explanation": "Нереальное условие в прошлом."
            }
            """;

        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(aiJson);

        com.linguaoptima.api.dto.request.ExplainSentenceRequest req = com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
            .sentence("If I had known, I would have told you.")
            .cefrLevel(CefrLevel.B2)
            .build();

        com.linguaoptima.api.dto.response.ExplainSentenceResponse res = taskService.explainSentence(req, studentUser);

        assertNotNull(res);
        assertEquals("If I had known, I would have told you.", res.getOriginalSentence());
        assertEquals("Если бы я знал, я бы сказал тебе.", res.getTranslation());
        assertTrue(res.getGrammarBreakdown().contains("Third Conditional"));
        assertEquals(2, res.getKeyVocabulary().size());
        assertEquals("Нереальное условие в прошлом.", res.getExplanation());
    }

    @Test
    void testExplainSentenceValidation() {
        assertThrows(IllegalArgumentException.class, () -> taskService.explainSentence(null, studentUser));

        com.linguaoptima.api.dto.request.ExplainSentenceRequest nullSentence = com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
            .sentence(null)
            .build();
        assertThrows(IllegalArgumentException.class, () -> taskService.explainSentence(nullSentence, studentUser));

        com.linguaoptima.api.dto.request.ExplainSentenceRequest blankSentence = com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
            .sentence("   ")
            .build();
        assertThrows(IllegalArgumentException.class, () -> taskService.explainSentence(blankSentence, studentUser));
    }

    @Test
    void testExplainSentenceFallbackOnAiMalformedJson() {
        when(aiBrokerService.generateTaskContent(anyString(), any())).thenReturn("not a valid json string");

        com.linguaoptima.api.dto.request.ExplainSentenceRequest req = com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
            .sentence("The weather is delightful today.")
            .build();

        // Call with null user to test level fallback to B1
        com.linguaoptima.api.dto.response.ExplainSentenceResponse res = taskService.explainSentence(req, null);

        assertNotNull(res);
        assertEquals("The weather is delightful today.", res.getOriginalSentence());
        assertNotNull(res.getTranslation());
        assertNotNull(res.getGrammarBreakdown());
        assertNotNull(res.getExplanation());
        assertTrue(res.getKeyVocabulary().isEmpty());
    }

    @Test
    void testGetTasksForGroupNotFoundThrows() {
        UUID unknownGroup = UUID.randomUUID();
        when(groupRepository.findById(unknownGroup)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTasksForGroup(unknownGroup, teacherUser));
    }

    @Test
    void testExplainSentence_UserCefrLevelUsedWhenRequestNull() {
        studentUser.setCefrLevel(CefrLevel.A2);
        String aiJson = """
            {
              "originalSentence": "He lives in London.",
              "translation": "Он живет в Лондоне.",
              "grammarBreakdown": "Present Simple, 3rd person singular",
              "keyVocabulary": ["lives — живет"],
              "explanation": "Окончание -s в 3-м лице."
            }
            """;
        when(aiBrokerService.generateTaskContent(anyString(), eq(studentUser))).thenReturn(aiJson);

        com.linguaoptima.api.dto.request.ExplainSentenceRequest req = com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
            .sentence("He lives in London.")
            .cefrLevel(null)
            .build();

        com.linguaoptima.api.dto.response.ExplainSentenceResponse res = taskService.explainSentence(req, studentUser);
        assertNotNull(res);
        assertEquals("He lives in London.", res.getOriginalSentence());
    }
}

