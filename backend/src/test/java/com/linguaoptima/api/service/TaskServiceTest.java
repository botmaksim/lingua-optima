/**
 * @file TaskServiceTest.java
 * @brief Unit and slice test suite for TaskService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.*;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.exception.ForbiddenException;
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
     * @brief Verifies unit test scenario: save as template non teacher throws.
     */
    @Test
    void testSaveAsTemplateNonTeacherThrows() {
        TaskParamsRequest req = TaskParamsRequest.builder().cefrLevel(CefrLevel.B1).taskType(TaskType.MCQ).build();
        assertThrows(ForbiddenException.class, () -> taskService.saveAsTemplate(req, studentUser));
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
}

