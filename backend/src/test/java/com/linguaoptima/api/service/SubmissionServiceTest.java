/**
 * @file SubmissionServiceTest.java
 * @brief Unit and slice test suite for SubmissionService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for SubmissionService.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Test fixture or mock dependency for ocr service. */
    @Mock
    private OCRService ocrService;
    /** @brief Test fixture or mock dependency for scoring service. */
    @Mock
    private ScoringService scoringService;
    /** @brief Test fixture or mock dependency for usage service. */
    @Mock
    private UsageService usageService;
    /** @brief Test fixture or mock dependency for progress service. */
    @Mock
    private ProgressService progressService;
    /** @brief Test fixture or mock dependency for gamification service. */
    @Mock
    private GamificationService gamificationService;
    /** @brief Test fixture or mock dependency for notification service. */
    @Mock
    private NotificationService notificationService;

    /** @brief Test fixture or mock dependency for object mapper. */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    /** @brief Test fixture or mock dependency for submission service. */
    @InjectMocks
    private SubmissionService submissionService;

    /** @brief Test fixture or mock dependency for student. */
    private User student;
    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;
    /** @brief Test fixture or mock dependency for assignment. */
    private TaskAssignment assignment;

    /**
     * @brief Initializes test fixtures and mock state before each test in SubmissionServiceTest.
     */
    @BeforeEach
    void setUp() {
        student = User.builder()
            .id(UUID.randomUUID())
            .email("student@lingua.com")
            .role(Role.STUDENT)
            .cefrLevel(CefrLevel.B1)
            .build();

        teacher = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@lingua.com")
            .fullName("Professor")
            .role(Role.TEACHER)
            .build();

        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Passive Voice")
            .answerKey("key")
            .build();

        assignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .build();
    }

    /**
     * @brief Verifies unit test scenario: submit text grammar.
     */
    @Test
    void testSubmitTextGrammar() {
        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .assignmentId(assignment.getId())
            .text("My homework answer")
            .type("GRAMMAR")
            .build();

        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(scoringService.scoreGrammarTask(eq("My homework answer"), anyString(), eq(student)))
            .thenReturn(Map.of("score", 85.0, "feedback", "Well done"));
        when(scoringService.extractScore(any())).thenReturn(85.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitText(req, student);

        assertNotNull(res);
        assertEquals(85.0, res.getScore());
        verify(usageService).incrementEvaluation(student);
        verify(gamificationService).onSubmissionCompleted(student);
        verify(progressService).updateFromSubmission(student, "Passive Voice", true);
    }

    /**
     * @brief Verifies unit test scenario: submit text essay.
     */
    @Test
    void testSubmitTextEssay() {
        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .text("An extensive essay text about climate change...")
            .type("ESSAY")
            .build();

        when(scoringService.scoreEssay(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of("overallScore", 8.0, "feedback", "Strong arguments"));
        when(scoringService.extractScore(any())).thenReturn(80.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitText(req, student);
        assertNotNull(res);
        assertEquals(80.0, res.getScore());
        verify(scoringService).scoreEssay(anyString(), eq("B1"), eq(student));
    }

    /**
     * @brief Verifies unit test scenario: submit image zero retention.
     */
    @Test
    void testSubmitImageZeroRetention() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2, 3});

        when(ocrService.extractText(any())).thenReturn("Recognized handwritten text");
        when(scoringService.scoreGrammarTask(eq("Recognized handwritten text"), anyString(), eq(student)))
            .thenReturn(Map.of("score", 90.0));
        when(scoringService.extractScore(any())).thenReturn(90.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitImage(file, null, student);

        assertNotNull(res);
        assertEquals(SubmissionType.IMAGE, res.getSubmissionType());
        assertEquals("Recognized handwritten text", res.getOriginalText());
        verify(usageService).incrementOcr(student);
        verify(usageService).incrementEvaluation(student);
    }

    /**
     * @brief Verifies unit test scenario: override score teacher.
     */
    @Test
    void testOverrideScoreTeacher() {
        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .aiScore(60.0)
            .build();

        OverrideRequest req = OverrideRequest.builder()
            .overrideScore(85.0)
            .teacherComment("Good effort, improved grade.")
            .build();

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(sub.getId(), req, teacher);

        assertNotNull(res);
        assertEquals(85.0, res.getOverrideScore());
        assertEquals("Good effort, improved grade.", res.getTeacherComment());
        verify(notificationService).send(eq(student), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: override score student throws.
     */
    @Test
    void testOverrideScoreStudentThrows() {
        OverrideRequest req = OverrideRequest.builder().overrideScore(100.0).build();
        assertThrows(ForbiddenException.class, () -> submissionService.overrideScore(UUID.randomUUID(), req, student));
    }

    /**
     * @brief Verifies unit test scenario: submit text assignment not found throws.
     */
    @Test
    void testSubmitTextAssignmentNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        TextSubmissionRequest req = TextSubmissionRequest.builder().assignmentId(randomId).text("Homework").build();
        when(taskAssignmentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.submitText(req, student));
    }

    /**
     * @brief Verifies unit test scenario: submit text forbidden student throws.
     */
    @Test
    void testSubmitTextForbiddenStudentThrows() {
        User otherStudent = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        TaskAssignment otherAssignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .student(otherStudent)
            .build();

        TextSubmissionRequest req = TextSubmissionRequest.builder().assignmentId(otherAssignment.getId()).text("Homework").build();
        when(taskAssignmentRepository.findById(otherAssignment.getId())).thenReturn(Optional.of(otherAssignment));

        assertThrows(ForbiddenException.class, () -> submissionService.submitText(req, student));
    }

    /**
     * @brief Verifies unit test scenario: submit image with assignment.
     */
    @Test
    void testSubmitImageWithAssignment() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2, 3});
        when(ocrService.extractText(any())).thenReturn("Handwritten math text");
        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(scoringService.scoreGrammarTask(eq("Handwritten math text"), anyString(), eq(student)))
            .thenReturn(Map.of("score", 95.0));
        when(scoringService.extractScore(any())).thenReturn(95.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitImage(file, assignment.getId(), student);
        assertNotNull(res);
        assertEquals(AssignmentStatus.SUBMITTED, assignment.getStatus());
    }

    /**
     * @brief Verifies unit test scenario: submit image assignment not found throws.
     */
    @Test
    void testSubmitImageAssignmentNotFoundThrows() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2});
        when(ocrService.extractText(any())).thenReturn("Text");
        UUID randomId = UUID.randomUUID();
        when(taskAssignmentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.submitImage(file, randomId, student));
    }

    /**
     * @brief Verifies unit test scenario: submit image assignment other student throws.
     */
    @Test
    void testSubmitImageAssignmentOtherStudentThrows() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2});
        when(ocrService.extractText(any())).thenReturn("Text");
        User otherStudent = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        TaskAssignment otherAssignment = TaskAssignment.builder().id(UUID.randomUUID()).student(otherStudent).build();
        when(taskAssignmentRepository.findById(otherAssignment.getId())).thenReturn(Optional.of(otherAssignment));

        assertThrows(ForbiddenException.class, () -> submissionService.submitImage(file, otherAssignment.getId(), student));
    }

    /**
     * @brief Verifies unit test scenario: submit image io exception throws ocr exception.
     */
    @Test
    void testSubmitImageIoExceptionThrowsOcrException() throws Exception {
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new java.io.IOException("Disk read error"));

        assertThrows(com.linguaoptima.api.exception.OcrException.class,
            () -> submissionService.submitImage(mockFile, null, student));
    }

    /**
     * @brief Verifies unit test scenario: override score with assignment.
     */
    @Test
    void testOverrideScoreWithAssignment() {
        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(assignment)
            .student(student)
            .aiScore(50.0)
            .build();

        OverrideRequest req = OverrideRequest.builder().overrideScore(90.0).teacherComment("Great update").build();
        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(sub.getId(), req, teacher);
        assertEquals(90.0, res.getOverrideScore());
        assertEquals(AssignmentStatus.GRADED, assignment.getStatus());
        verify(taskAssignmentRepository).save(assignment);
    }

    /**
     * @brief Verifies unit test scenario: override score not found throws.
     */
    @Test
    void testOverrideScoreNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        OverrideRequest req = OverrideRequest.builder().overrideScore(90.0).build();
        when(submissionRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.overrideScore(randomId, req, teacher));
    }

    /**
     * @brief Verifies unit test scenario: get submission by id not found throws.
     */
    @Test
    void testGetSubmissionByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        when(submissionRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.getSubmissionById(randomId, student));
    }

    /**
     * @brief Verifies unit test scenario: get submissions.
     */
    @Test
    void testGetSubmissions() {
        Submission sub = Submission.builder().id(UUID.randomUUID()).student(student).build();
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of(sub));
        when(submissionRepository.findAll()).thenReturn(List.of(sub));
        when(submissionRepository.findActiveGroupSubmissions(any())).thenReturn(List.of(sub));
        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        assertEquals(1, submissionService.getMySubmissions(student).size());
        assertEquals(1, submissionService.getMySubmissions(teacher).size());
        assertEquals(1, submissionService.getGroupSubmissions(UUID.randomUUID(), teacher).size());
        assertThrows(ForbiddenException.class, () -> submissionService.getGroupSubmissions(UUID.randomUUID(), student));
        assertEquals(sub.getId(), submissionService.getSubmissionById(sub.getId(), student).getId());
        assertEquals(sub.getId(), submissionService.getSubmissionById(sub.getId(), teacher).getId());

        User stranger = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        assertThrows(ForbiddenException.class, () -> submissionService.getSubmissionById(sub.getId(), stranger));
    }
}
