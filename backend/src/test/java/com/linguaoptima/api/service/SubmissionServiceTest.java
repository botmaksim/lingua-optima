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
 * @file SubmissionServiceTest.java
 * @brief Unit and slice test suite for SubmissionService.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private OCRService ocrService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private UsageService usageService;
    @Mock
    private ProgressService progressService;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SubmissionService submissionService;

    private User student;
    private User teacher;
    private TaskAssignment assignment;

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

    @Test
    void testOverrideScoreStudentThrows() {
        OverrideRequest req = OverrideRequest.builder().overrideScore(100.0).build();
        assertThrows(ForbiddenException.class, () -> submissionService.overrideScore(UUID.randomUUID(), req, student));
    }

    @Test
    void testSubmitTextAssignmentNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        TextSubmissionRequest req = TextSubmissionRequest.builder().assignmentId(randomId).text("Homework").build();
        when(taskAssignmentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.submitText(req, student));
    }

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

    @Test
    void testSubmitImageAssignmentNotFoundThrows() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2});
        when(ocrService.extractText(any())).thenReturn("Text");
        UUID randomId = UUID.randomUUID();
        when(taskAssignmentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.submitImage(file, randomId, student));
    }

    @Test
    void testSubmitImageAssignmentOtherStudentThrows() {
        MockMultipartFile file = new MockMultipartFile("file", "homework.png", "image/png", new byte[]{1, 2});
        when(ocrService.extractText(any())).thenReturn("Text");
        User otherStudent = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        TaskAssignment otherAssignment = TaskAssignment.builder().id(UUID.randomUUID()).student(otherStudent).build();
        when(taskAssignmentRepository.findById(otherAssignment.getId())).thenReturn(Optional.of(otherAssignment));

        assertThrows(ForbiddenException.class, () -> submissionService.submitImage(file, otherAssignment.getId(), student));
    }

    @Test
    void testSubmitImageIoExceptionThrowsOcrException() throws Exception {
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new java.io.IOException("Disk read error"));

        assertThrows(com.linguaoptima.api.exception.OcrException.class,
            () -> submissionService.submitImage(mockFile, null, student));
    }

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

    @Test
    void testOverrideScoreNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        OverrideRequest req = OverrideRequest.builder().overrideScore(90.0).build();
        when(submissionRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.overrideScore(randomId, req, teacher));
    }

    @Test
    void testGetSubmissionByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        when(submissionRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> submissionService.getSubmissionById(randomId, student));
    }

    @Test
    void testGetSubmissions() {
        Submission sub = Submission.builder().id(UUID.randomUUID()).student(student).build();
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of(sub));
        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        assertEquals(1, submissionService.getMySubmissions(student).size());
        assertEquals(sub.getId(), submissionService.getSubmissionById(sub.getId(), student).getId());
        assertEquals(sub.getId(), submissionService.getSubmissionById(sub.getId(), teacher).getId());

        User stranger = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        assertThrows(ForbiddenException.class, () -> submissionService.getSubmissionById(sub.getId(), stranger));
    }
}
