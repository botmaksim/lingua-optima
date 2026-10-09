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
 * @file TaskServiceTest.java
 * @brief Unit and slice test suite for TaskService.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private AIBrokerService aiBrokerService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private UsageService usageService;
    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskService taskService;

    private User studentUser;
    private User teacherUser;

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
                { "text": "If it rains, we ___ inside.", "correctAnswer": "stay", "options": ["stay", "will stay"], "difficulty": 2, "grammarRule": "Zero Conditional" }
              ],
              "answerKey": "[{\\"id\\":1}]"
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

    @Test
    void testSaveAsTemplateNonTeacherThrows() {
        TaskParamsRequest req = TaskParamsRequest.builder().cefrLevel(CefrLevel.B1).taskType(TaskType.MCQ).build();
        assertThrows(ForbiddenException.class, () -> taskService.saveAsTemplate(req, studentUser));
    }

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

    @Test
    void testAssignTaskTaskNotFoundThrows() {
        UUID unknown = UUID.randomUUID();
        when(taskRepository.findById(unknown)).thenReturn(Optional.empty());
        AssignTaskRequest req = AssignTaskRequest.builder().groupIds(List.of(UUID.randomUUID())).build();

        assertThrows(ResourceNotFoundException.class, () -> taskService.assignTask(unknown, req, teacherUser));
    }

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
}
