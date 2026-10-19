/**
 * @file SubmissionServiceTest.java
 * @brief Unit and slice test suite for SubmissionService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.TaskQuestion;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.domain.enums.TaskType;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.SentenceCorrectionResponse;
import com.linguaoptima.api.dto.response.SubmissionItemResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.OcrException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.SessionStateRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import com.linguaoptima.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionServiceTest {

    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Test fixture or mock dependency for task repository. */
    @Mock
    private TaskRepository taskRepository;
    /** @brief Test fixture or mock dependency for session state repository. */
    @Mock
    private SessionStateRepository sessionStateRepository;
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
    }

    /**
     * @brief Verifies unit test scenario: submit text with taskId creating self-assignment.
     */
    @Test
    void testSubmitTextWithTaskId() {
        Task t = Task.builder()
            .id(UUID.randomUUID())
            .createdBy(student)
            .grammarTopic("Reported Speech")
            .answerKey("[{\"questionOrder\": 1, \"correctOption\": \"said\"}]")
            .build();
        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .taskId(t.getId())
            .text("She said that...")
            .type("GRAMMAR")
            .build();

        when(taskRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(taskAssignmentRepository.findByStudentIdAndTaskId(student.getId(), t.getId())).thenReturn(Optional.empty());
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);
        when(scoringService.scoreGrammarTask(eq("She said that..."), eq(t.getAnswerKey()), eq(student)))
            .thenReturn(Map.of("score", 90.0, "feedback", "Great"));
        when(scoringService.extractScore(any())).thenReturn(90.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitText(req, student);
        assertNotNull(res);
        assertEquals(90.0, res.getScore());
        verify(progressService).updateFromTaskSubmission(eq(student), eq("Reported Speech"), anyInt(), anyInt());

        // Test with createdBy == null
        t.setCreatedBy(null);
        SubmissionResultResponse resNullCreatedBy = submissionService.submitText(req, student);
        assertNotNull(resNullCreatedBy);

        // Test when taskId is provided but task is not found
        UUID notFoundId = UUID.randomUUID();
        TextSubmissionRequest reqNotFound = TextSubmissionRequest.builder()
            .taskId(notFoundId)
            .text("Some other text")
            .type("GRAMMAR")
            .build();
        when(taskRepository.findById(notFoundId)).thenReturn(Optional.empty());
        SubmissionResultResponse resNotFound = submissionService.submitText(reqNotFound, student);
        assertNotNull(resNotFound);
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
        verify(usageService).incrementOcr(student, 1);
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
            .feedback("Updated AI feedback by teacher.")
            .build();

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(sub.getId(), req, teacher);

        assertNotNull(res);
        assertEquals(85.0, res.getOverrideScore());
        assertEquals("Good effort, improved grade.", res.getTeacherComment());
        assertEquals("Updated AI feedback by teacher.", res.getFeedback());
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

    /**
     * @brief Verifies non-obvious boundary conditions: 69.99 vs 70.0 pass threshold, null AI score, detached null task on assignment, and ADMIN role privileges.
     */
    @Test
    void testPassFailBoundariesNullTaskAndAdminRole() {
        User admin = User.builder().id(UUID.randomUUID()).email("admin@lingua.com").fullName("Admin").role(Role.ADMIN).build();
        TaskAssignment detachedAssignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(null)
            .student(student)
            .maxAttempts(0)
            .build();

        when(taskAssignmentRepository.findById(detachedAssignment.getId())).thenReturn(Optional.of(detachedAssignment));
        when(scoringService.scoreGrammarTask(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of("score", 69.99));
        when(scoringService.extractScore(any())).thenReturn(69.99).thenReturn(null);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });

        TextSubmissionRequest failTextReq = TextSubmissionRequest.builder()
            .assignmentId(detachedAssignment.getId())
            .text("Almost passing")
            .type("GRAMMAR")
            .build();
        submissionService.submitText(failTextReq, student);
        verify(progressService).updateFromTaskSubmission(eq(student), eq("General"), anyInt(), anyInt());

        submissionService.submitText(failTextReq, student);
        verify(progressService, times(2)).updateFromTaskSubmission(eq(student), eq("General"), anyInt(), anyInt());

        MockMultipartFile file = new MockMultipartFile("file", "hw.png", "image/png", new byte[]{1, 2, 3});
        when(ocrService.extractText(any())).thenReturn("Handwriting");
        when(scoringService.extractScore(any())).thenReturn(69.9).thenReturn(null);

        submissionService.submitImage(file, detachedAssignment.getId(), student);
        verify(progressService).updateFromTaskSubmission(eq(student), eq("OCR Homework"), anyInt(), anyInt());

        submissionService.submitImage(file, null, student);
        verify(progressService, times(2)).updateFromTaskSubmission(eq(student), eq("OCR Homework"), anyInt(), anyInt());

        Submission sub = Submission.builder().id(UUID.randomUUID()).student(student).aiScore(50.0).build();
        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(submissionRepository.findAll()).thenReturn(List.of(sub));
        when(submissionRepository.findActiveGroupSubmissions(any())).thenReturn(List.of(sub));

        OverrideRequest overrideReq = OverrideRequest.builder().overrideScore(95.0).teacherComment("Admin review").build();
        assertEquals(95.0, submissionService.overrideScore(sub.getId(), overrideReq, admin).getOverrideScore());
        assertEquals(1, submissionService.getMySubmissions(admin).size());
        assertEquals(1, submissionService.getGroupSubmissions(UUID.randomUUID(), admin).size());
        assertEquals(sub.getId(), submissionService.getSubmissionById(sub.getId(), admin).getId());
    }

    /**
     * @brief Tests enrichment of submission results with task questions, CAT session, and rubric corrections.
     */
    @Test
    void testEnrichSubmissionResultWithTaskQuestionsAndCatSessionAndRubric() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Past Simple")
            .answerKey("[{\"questionOrder\":1,\"explanation\":\"Irregular past form of wake\"}]")
            .build();

        com.linguaoptima.api.domain.TaskQuestion q1 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(1)
            .questionText("He ___ (wake) up early.")
            .correctAnswer("woke")
            .grammarRule("Past Simple Irregular Verbs")
            .optionsJson("[\"woke\", \"waked\", \"woken\"]")
            .points(15)
            .build();

        com.linguaoptima.api.domain.TaskQuestion q2 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(2)
            .questionText("She ___ (go) home.")
            .correctAnswer("went / had gone")
            .grammarRule("Past Simple Auxiliary Usage")
            .optionsJson("invalid-json")
            .points(25)
            .build();

        task.setQuestions(List.of(q1, q2));

        TaskAssignment assign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .build();

        Submission subWithTask = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(assign)
            .student(student)
            .studentText("Q1: woke\nQ2: did went")
            .aiScore(80.0)
            .aiFeedback("Good effort.")
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(subWithTask);
        submissionService.enrichSubmissionResult(resp, subWithTask, null);

        assertNotNull(resp.getItems());
        assertEquals(2, resp.getItems().size());
        assertTrue(resp.getItems().get(0).isCorrect());
        assertEquals("woke", resp.getItems().get(0).getStudentAnswer());
        assertEquals("Irregular past form of wake", resp.getItems().get(0).getExplanation());
        assertEquals(List.of("woke", "waked", "woken"), resp.getItems().get(0).getOptions());
        assertEquals(15, resp.getItems().get(0).getPoints());
        assertEquals(25, resp.getItems().get(1).getPoints());
        assertTrue(resp.getItems().get(1).getOptions().isEmpty());

        assertFalse(resp.getItems().get(1).isCorrect());
        assertEquals("did went", resp.getItems().get(1).getStudentAnswer());
        assertNotNull(resp.getAiAnalysis());
        assertTrue(resp.getAiAnalysis().getWeaknesses().contains("Past Simple Auxiliary Usage"));
        assertTrue(resp.getAiAnalysis().getStrengths().contains("Past Simple Irregular Verbs"));

        // Test CAT session enrichment
        com.linguaoptima.api.domain.SessionState catSession = com.linguaoptima.api.domain.SessionState.builder()
            .answersJson("[{\"questionText\":\"CAT Q1\",\"answer\":\"A\",\"correctAnswer\":\"A\",\"isCorrect\":true,\"grammarRule\":\"CAT Rule\",\"difficulty\":3,\"options\":[\"A\",\"B\"]}]")
            .build();
        when(sessionStateRepository.findByAssignmentId(assign.getId())).thenReturn(Optional.of(catSession));

        SubmissionResultResponse catResp = SubmissionResultResponse.fromEntity(subWithTask);
        submissionService.enrichSubmissionResult(catResp, subWithTask, null);
        assertEquals(1, catResp.getItems().size());
        assertEquals("CAT Q1", catResp.getItems().get(0).getSentence());
        assertEquals(List.of("A", "B"), catResp.getItems().get(0).getOptions());

        // Test rubric corrections and AI feedback JSON parsing
        when(sessionStateRepository.findByAssignmentId(assign.getId())).thenReturn(Optional.empty());
        Map<String, Object> rubric = Map.of(
            "corrections", List.of(Map.of("original", "bad", "corrected", "good", "explanation", "fix", "grammarRule", "Syntax")),
            "weaknesses", List.of("Essay Weakness"),
            "strengths", List.of("Essay Strength")
        );
        SubmissionResultResponse rubricResp = SubmissionResultResponse.fromEntity(subWithTask);
        submissionService.enrichSubmissionResult(rubricResp, subWithTask, rubric);
        assertFalse(rubricResp.getCorrections().isEmpty());
        assertEquals("bad", rubricResp.getCorrections().get(0).getOriginal());
        assertTrue(rubricResp.getAiAnalysis().getWeaknesses().contains("Essay Weakness"));
    }

    /**
     * @brief Tests fallback branches, null cases, edge cases, and JSON error handling in enrichSubmissionResult.
     */
    @Test
    void testEnrichSubmissionResultEdgeCasesAndFallbacks() {
        // 1. Assignment is null
        Submission subNoAssign = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("Hello world")
            .aiScore(75.0)
            .aiFeedback("{\"corrections\":[{\"original\":\"bad\",\"corrected\":\"good\",\"explanation\":\"fix\",\"grammarRule\":\"rule\"}],\"weaknesses\":[\"W1\"],\"strengths\":[\"S1\"]}")
            .build();
        SubmissionResultResponse respNoAssign = SubmissionResultResponse.fromEntity(subNoAssign);
        submissionService.enrichSubmissionResult(respNoAssign, subNoAssign, null);
        assertNotNull(respNoAssign.getCorrections());
        assertEquals(1, respNoAssign.getCorrections().size());
        assertTrue(respNoAssign.getAiAnalysis().getSummary().contains("Essay evaluated against CEFR criteria"));
        assertTrue(respNoAssign.getAiAnalysis().getRecommendations().contains("W1"));

        // 2. No items, no corrections, no weaknesses, but has strengths
        Submission subStrengthsOnly = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("Clean essay")
            .aiScore(100.0)
            .aiFeedback("{\"strengths\":[\"Punctuation\"]}")
            .build();
        SubmissionResultResponse respStrengthsOnly = SubmissionResultResponse.fromEntity(subStrengthsOnly);
        submissionService.enrichSubmissionResult(respStrengthsOnly, subStrengthsOnly, null);
        assertTrue(respStrengthsOnly.getAiAnalysis().getSummary().contains("Overall score: 100/100"));
        assertTrue(respStrengthsOnly.getAiAnalysis().getRecommendations().contains("Punctuation"));
        assertEquals(List.of("Grammar & Sentence Structure"), respStrengthsOnly.getAiAnalysis().getSuggestedTopics());

        // 3. Neither weaknesses nor strengths
        Submission subEmptyFeedback = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("Empty")
            .aiScore(90.0)
            .aiFeedback("Good effort without json.")
            .build();
        SubmissionResultResponse respEmpty = SubmissionResultResponse.fromEntity(subEmptyFeedback);
        submissionService.enrichSubmissionResult(respEmpty, subEmptyFeedback, null);
        assertTrue(respEmpty.getAiAnalysis().getRecommendations().contains("Great effort!"));

        // 4. Task with null questions, empty questions
        Task emptyTask = Task.builder().id(UUID.randomUUID()).questions(null).build();
        TaskAssignment assignEmptyTask = TaskAssignment.builder().id(UUID.randomUUID()).task(emptyTask).build();
        Submission subEmptyTask = Submission.builder().id(UUID.randomUUID()).assignment(assignEmptyTask).student(student).studentText("").build();
        SubmissionResultResponse respEmptyTask = SubmissionResultResponse.fromEntity(subEmptyTask);
        submissionService.enrichSubmissionResult(respEmptyTask, subEmptyTask, null);
        assertTrue(respEmptyTask.getItems().isEmpty());

        // 5. Questions with no grammar rule, task with no grammar topic, blank student answers, comma-delimited correct answers
        Task customTask = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic(null)
            .answerKey("[{\"questionId\":1,\"explanation\":\"\"},{\"invalidJson")
            .build();
        com.linguaoptima.api.domain.TaskQuestion tq1 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(1)
            .questionText("Fill in comma-delimited")
            .correctAnswer("alpha, beta")
            .grammarRule(null)
            .build();
        com.linguaoptima.api.domain.TaskQuestion tq2 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(2)
            .questionText("Fill in slash-delimited")
            .correctAnswer("gamma / delta")
            .grammarRule("")
            .build();
        com.linguaoptima.api.domain.TaskQuestion tq3 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(3)
            .questionText("No student answer")
            .correctAnswer("omega")
            .grammarRule("Omega Rule")
            .build();
        customTask.setQuestions(List.of(tq1, tq2, tq3));

        TaskAssignment customAssign = TaskAssignment.builder().id(UUID.randomUUID()).task(customTask).build();
        Submission subCustom = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(customAssign)
            .student(student)
            .studentText("   \nalpha\nQuestion 2: gamma\n")
            .aiScore(66.0)
            .aiFeedback("{not valid json")
            .build();

        SubmissionResultResponse respCustom = SubmissionResultResponse.fromEntity(subCustom);
        submissionService.enrichSubmissionResult(respCustom, subCustom, null);
        assertEquals(3, respCustom.getItems().size());
        assertTrue(respCustom.getItems().get(0).isCorrect());
        assertTrue(respCustom.getItems().get(1).isCorrect());
        assertFalse(respCustom.getItems().get(2).isCorrect());
        assertEquals("No answer", respCustom.getItems().get(2).getStudentAnswer());
        assertTrue(respCustom.getItems().get(2).isSkipped());
        assertTrue(respCustom.getItems().get(2).getExplanation().contains("The expected answer is 'omega'"));

        // 6. CAT session with incorrect answers and malformed JSON
        com.linguaoptima.api.domain.SessionState catFail = com.linguaoptima.api.domain.SessionState.builder()
            .answersJson("[{\"questionText\":\"CAT Hard Q\",\"answer\":\"wrong\",\"correctAnswer\":\"right\",\"isCorrect\":false,\"grammarRule\":\"Conditionals\",\"difficulty\":4}]")
            .build();
        when(sessionStateRepository.findByAssignmentId(customAssign.getId())).thenReturn(Optional.of(catFail));
        SubmissionResultResponse respCatFail = SubmissionResultResponse.fromEntity(subCustom);
        submissionService.enrichSubmissionResult(respCatFail, subCustom, null);
        assertEquals(1, respCatFail.getItems().size());
        assertFalse(respCatFail.getItems().get(0).isCorrect());
        assertTrue(respCatFail.getItems().get(0).getExplanation().contains("The expected answer is 'right'"));

        // Malformed CAT JSON
        com.linguaoptima.api.domain.SessionState catMalformed = com.linguaoptima.api.domain.SessionState.builder()
            .answersJson("{invalid json")
            .build();
        when(sessionStateRepository.findByAssignmentId(customAssign.getId())).thenReturn(Optional.of(catMalformed));
        SubmissionResultResponse respCatMalformed = SubmissionResultResponse.fromEntity(subCustom);
        submissionService.enrichSubmissionResult(respCatMalformed, subCustom, null);
        // Should fall back to task questions since CAT parsing produced 0 items
        assertEquals(3, respCatMalformed.getItems().size());

        // 7. Grammar topic with no weaknesses
        Submission subTopicOnly = Submission.builder().id(UUID.randomUUID()).build();
        SubmissionResultResponse respTopicOnly = SubmissionResultResponse.builder()
            .grammarTopic("Reported Speech")
            .build();
        submissionService.enrichSubmissionResult(respTopicOnly, subTopicOnly, null);
        assertEquals(List.of("Reported Speech (Advanced Practice)"), respTopicOnly.getAiAnalysis().getSuggestedTopics());

        // 8. Null guard checks
        submissionService.enrichSubmissionResult(null, subCustom, null);
        submissionService.enrichSubmissionResult(respTopicOnly, null, null);

        // 9. Comma/slash matching second variants, number overflow fallback, and empty strings
        com.linguaoptima.api.domain.TaskQuestion emptyQ = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(1)
            .questionText("Empty Q")
            .correctAnswer("")
            .build();
        com.linguaoptima.api.domain.TaskQuestion commaQ = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(2)
            .questionText("Comma Q")
            .correctAnswer("first, second")
            .build();
        Task matchTask = Task.builder().id(UUID.randomUUID()).questions(List.of(emptyQ, commaQ)).build();
        TaskAssignment matchAssign = TaskAssignment.builder().id(UUID.randomUUID()).task(matchTask).build();
        Submission matchSub = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(matchAssign)
            .student(student)
            .studentText("Q1: \nQ2: second\nQ999999999999999999999999: BigNum\n")
            .build();
        SubmissionResultResponse matchResp = SubmissionResultResponse.fromEntity(matchSub);
        submissionService.enrichSubmissionResult(matchResp, matchSub, null);
        assertEquals(2, matchResp.getItems().size());
        assertFalse(matchResp.getItems().get(0).isCorrect());
        assertTrue(matchResp.getItems().get(1).isCorrect());
    }

    @Test
    void testSubmissionAttemptLimitsEnforcement() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Conditionals")
            .answerKey("[]")
            .build();

        // 1. Existing assignment via taskId with 1 attempt already used -> throws ForbiddenException
        TaskAssignment exhaustedAssign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .maxAttempts(1)
            .attemptsUsed(1)
            .status(AssignmentStatus.SUBMITTED)
            .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskAssignmentRepository.findByStudentIdAndTaskId(student.getId(), task.getId()))
            .thenReturn(Optional.of(exhaustedAssign));

        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .taskId(task.getId())
            .text("Q1: answer")
            .type("GRAMMAR")
            .build();

        assertThrows(ForbiddenException.class, () -> submissionService.submitText(req, student));

        // 2. Unlimited attempts (maxAttempts = 0) or null maxAttempts succeeds even when attemptsUsed >= 1
        exhaustedAssign.setMaxAttempts(0);
        when(scoringService.scoreGrammarTask(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of("score", 80.0, "feedback", "Good"));
        when(scoringService.extractScore(any())).thenReturn(80.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse unlimRes = submissionService.submitText(req, student);
        assertNotNull(unlimRes);
        assertEquals(2, exhaustedAssign.getAttemptsUsed());

        exhaustedAssign.setMaxAttempts(null);
        SubmissionResultResponse nullMaxRes = submissionService.submitText(req, student);
        assertNotNull(nullMaxRes);
        assertEquals(3, exhaustedAssign.getAttemptsUsed());
    }

    /**
     * @brief Verifies unit test scenario: submitting a task with explicit questions records progress with actual question count and score-derived errors.
     */
    @Test
    void testSubmitTaskWithExplicitQuestionsAndScore() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Present Perfect vs Past Simple")
            .answerKey("key")
            .questions(List.of(
                TaskQuestion.builder().questionOrder(1).questionText("Q1").correctAnswer("went").grammarRule("Past Simple").build(),
                TaskQuestion.builder().questionOrder(2).questionText("Q2").correctAnswer("have seen").grammarRule("Present Perfect").build(),
                TaskQuestion.builder().questionOrder(3).questionText("Q3").correctAnswer("woke").grammarRule("Past Simple").build(),
                TaskQuestion.builder().questionOrder(4).questionText("Q4").correctAnswer("has visited").grammarRule("Present Perfect").build(),
                TaskQuestion.builder().questionOrder(5).questionText("Q5").correctAnswer("did").grammarRule("Past Simple").build()
            ))
            .build();

        TaskAssignment assign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .maxAttempts(0)
            .attemptsUsed(0)
            .build();

        when(taskAssignmentRepository.findById(assign.getId())).thenReturn(Optional.of(assign));
        when(scoringService.scoreGrammarTask(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of("score", 80.0, "feedback", "4/5 correct"));
        when(scoringService.extractScore(any())).thenReturn(80.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .assignmentId(assign.getId())
            .text("1. went\n2. saw\n3. woke\n4. has visited\n5. did")
            .type("GRAMMAR")
            .build();

        SubmissionResultResponse res = submissionService.submitText(req, student);
        assertNotNull(res);
        assertEquals(80.0, res.getScore());
        verify(progressService).updateFromTaskSubmission(eq(student), eq("Present Perfect vs Past Simple"), eq(5), eq(1));
    }

    /**
     * @brief Verifies unit test scenario: extracting items from rubric map when questions are not bound to entity.
     */
    @Test
    void testExtractRubricItemsAndNullScoreFallback() {
        java.util.Map<String, Object> item3 = new java.util.HashMap<>();
        item3.put("sentence", "Sentence 3");
        item3.put("explanation", "Good explanation");

        java.util.Map<String, Object> corr = new java.util.HashMap<>();
        corr.put("original", "bad original");
        corr.put("corrected", "good correction");
        corr.put("explanation", "explanation text");
        corr.put("grammarRule", "rule text");

        java.util.Map<String, Object> rubricWithItems = new java.util.HashMap<>();
        rubricWithItems.put("score", 70.0);
        rubricWithItems.put("weaknesses", java.util.Arrays.asList("weak1", "", null));
        rubricWithItems.put("strengths", java.util.Arrays.asList("strong1", " ", null));
        rubricWithItems.put("corrections", List.of(corr));
        rubricWithItems.put("items", List.of(
            Map.of("questionNumber", 1, "sentence", "Sentence 1", "studentAnswer", "a", "correctAnswer", "a", "isCorrect", true, "grammarRule", "Rule 1"),
            item3
        ));

        SubmissionResultResponse res = SubmissionResultResponse.builder().build();
        Submission sub = Submission.builder().id(UUID.randomUUID()).build();
        submissionService.enrichSubmissionResult(res, sub, rubricWithItems);

        assertNotNull(res.getItems());
        assertEquals(2, res.getItems().size());
        assertTrue(res.getItems().get(0).isCorrect());
        assertFalse(res.getItems().get(1).isCorrect());
    }

    /**
     * @brief Verifies unit test scenario: submitting text when AI score is null falls back to counting item accuracy.
     */
    @Test
    void testSubmitTextNullScoreWithRubricItems() {
        when(scoringService.scoreGrammarTask(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of(
                "items", List.of(
                    Map.of("questionNumber", 1, "sentence", "Sentence 1", "studentAnswer", "a", "correctAnswer", "a", "isCorrect", true),
                    Map.of("questionNumber", 2, "sentence", "Sentence 2", "studentAnswer", "b", "correctAnswer", "c", "isCorrect", false)
                )
            ));
        when(scoringService.extractScore(any())).thenReturn(null);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .text("raw text")
            .type("GRAMMAR")
            .build();

        SubmissionResultResponse res = submissionService.submitText(req, student);
        assertNotNull(res);
        assertNull(res.getScore());
        assertEquals(2, res.getItems().size());
        verify(progressService).updateFromTaskSubmission(eq(student), eq("General"), eq(2), eq(1));
    }

    /**
     * @brief Verifies unit test scenario: retrieving educator submissions queue successfully.
     */
    @Test
    void testGetTeacherSubmissionsSuccess() {
        Submission studentSub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .aiScore(88.0)
            .build();

        when(submissionRepository.findTeacherStudentSubmissions(teacher.getId()))
            .thenReturn(List.of(studentSub));

        List<SubmissionResultResponse> teacherQueue = submissionService.getTeacherSubmissions(teacher);
        assertNotNull(teacherQueue);
        assertEquals(1, teacherQueue.size());
        assertEquals(studentSub.getId(), teacherQueue.get(0).getId());

        User adminUser = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        when(submissionRepository.findTeacherStudentSubmissions(adminUser.getId()))
            .thenReturn(List.of(studentSub));
        List<SubmissionResultResponse> adminQueue = submissionService.getTeacherSubmissions(adminUser);
        assertEquals(1, adminQueue.size());
    }

    /**
     * @brief Verifies unit test scenario: student attempting to access educator submissions queue is rejected.
     */
    @Test
    void testGetTeacherSubmissionsForbiddenForStudent() {
        assertThrows(ForbiddenException.class, () -> submissionService.getTeacherSubmissions(student));
    }

    /**
     * @brief Verifies unit test scenario: synthesizing itemized questions from numbered student text fallback.
     */
    @Test
    void testEnrichFromParsedStudentAnswersFallback() {
        Submission subNumbered = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("Q1: must be examined Q2: have presented")
            .aiScore(60.0)
            .aiFeedback("Q2 is incorrect. Review auxiliary verb.")
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(subNumbered);
        submissionService.enrichSubmissionResult(resp, subNumbered, null);

        assertNotNull(resp.getItems());
        assertEquals(2, resp.getItems().size());
        assertEquals("must be examined", resp.getItems().get(0).getStudentAnswer());
        assertTrue(resp.getItems().get(0).isCorrect());
        assertEquals("have presented", resp.getItems().get(1).getStudentAnswer());
        assertFalse(resp.getItems().get(1).isCorrect());
    }

    /**
     * @brief Verifies unit test scenario: score override with null teacher comment and null feedback.
     */
    @Test
    void testOverrideScoreNullCommentAndFeedback() {
        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .aiScore(60.0)
            .build();
        OverrideRequest req = OverrideRequest.builder().overrideScore(70.0).build();
        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(sub.getId(), req, teacher);
        assertEquals(70.0, res.getOverrideScore());
    }

    /**
     * @brief Verifies strict mutual exclusivity between strengths and weaknesses,
     * accurate attempt metrics calculation, and proper diagnostic recommendations.
     */
    @Test
    void testDiagnosticSegregationAndAttemptMetrics() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Conditionals")
            .build();

        com.linguaoptima.api.domain.TaskQuestion q1 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(1).questionText("Q1").correctAnswer("ans1").grammarRule("Conditional 0").build();
        com.linguaoptima.api.domain.TaskQuestion q2 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(2).questionText("Q2").correctAnswer("ans2").grammarRule("Conditional 1").build();
        com.linguaoptima.api.domain.TaskQuestion q3 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(3).questionText("Q3").correctAnswer("ans3").grammarRule("Conditional 2").build();
        com.linguaoptima.api.domain.TaskQuestion q4 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(4).questionText("Q4").correctAnswer("correct4").grammarRule("Conditional 1").build();
        com.linguaoptima.api.domain.TaskQuestion q5 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(5).questionText("Q5").correctAnswer("correct5").grammarRule("Conditional 0").build();
        com.linguaoptima.api.domain.TaskQuestion q6 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(6).questionText("Q6").correctAnswer("correct6").grammarRule("Conditional 2").build();
        com.linguaoptima.api.domain.TaskQuestion q7 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(7).questionText("Q7").correctAnswer("correct7").grammarRule("Conditional 1").build();
        com.linguaoptima.api.domain.TaskQuestion q8 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(8).questionText("Q8").correctAnswer("correct8").grammarRule("Conditional 0").build();
        com.linguaoptima.api.domain.TaskQuestion q9 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(9).questionText("Q9").correctAnswer("correct9").grammarRule("Conditional 0").build();
        com.linguaoptima.api.domain.TaskQuestion q10 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(10).questionText("Q10").correctAnswer("correct10").grammarRule("Conditional 2").build();

        task.setQuestions(List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10));

        TaskAssignment assign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .build();

        // Student answers Q1-Q3 correctly, Q4-Q5 incorrectly, Q6-Q10 left unanswered
        String studentText = "Q1: ans1\nQ2: ans2\nQ3: ans3\nQ4: wrong4\nQ5: wrong5\nQ6: No answer\nQ7: No answer\nQ8: No answer\nQ9: No answer\nQ10: No answer";

        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(assign)
            .student(student)
            .studentText(studentText)
            .aiScore(30.0)
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(sub);
        submissionService.enrichSubmissionResult(resp, sub, null);

        assertNotNull(resp.getAiAnalysis());
        var analysis = resp.getAiAnalysis();

        // Check metrics: 10 questions total, 5 attempted, 3 correct, 5 skipped
        assertEquals(10, analysis.getTotalQuestions());
        assertEquals(5, analysis.getAttemptedCount());
        assertEquals(3, analysis.getCorrectCount());
        assertEquals(5, analysis.getSkippedCount());
        assertEquals(60.0, analysis.getAttemptAccuracy());

        // Weaknesses should contain Conditional 0 and Conditional 1 (where mistakes occurred)
        assertTrue(analysis.getWeaknesses().contains("Conditional 0"));
        assertTrue(analysis.getWeaknesses().contains("Conditional 1"));

        // Mastered concepts (strengths) MUST NOT contain Conditional 0, Conditional 1, or Conditional 2
        // because Conditional 0 & 1 had errors, and Conditional 2 had skipped questions!
        assertFalse(analysis.getStrengths().contains("Conditional 0"));
        assertFalse(analysis.getStrengths().contains("Conditional 1"));
        assertFalse(analysis.getStrengths().contains("Conditional 2"));

        // Strict mutual exclusivity
        for (String w : analysis.getWeaknesses()) {
            assertFalse(analysis.getStrengths().contains(w), "Strengths must never contain weakness: " + w);
        }

        // Suggested topic must target an actual weakness
        assertFalse(analysis.getSuggestedTopics().isEmpty());
        assertTrue(analysis.getWeaknesses().contains(analysis.getSuggestedTopics().get(0)));

        // Summary must include both overall score and attempt accuracy
        assertTrue(analysis.getSummary().contains("3 out of 10 questions correctly (30% overall score)"));
        assertTrue(analysis.getSummary().contains("Attempt accuracy: 60%"));
        assertTrue(analysis.getSummary().contains("5 skipped"));
    }

    /**
     * @brief Verifies unit test scenario: submit images validations and multi-page processing.
     */
    @Test
    void testSubmitImagesSuccessAndValidations() {
        // 1. Null / empty file list
        assertThrows(OcrException.class, () -> submissionService.submitImages(null, assignment.getId(), student));
        assertThrows(OcrException.class, () -> submissionService.submitImages(List.of(), assignment.getId(), student));
        MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);
        assertThrows(OcrException.class, () -> submissionService.submitImages(List.of(emptyFile), assignment.getId(), student));

        // 2. Too many photos (> 5)
        MockMultipartFile smallFile = new MockMultipartFile("files", "p.png", "image/png", new byte[]{1, 2, 3});
        List<org.springframework.web.multipart.MultipartFile> sixFiles = List.of(smallFile, smallFile, smallFile, smallFile, smallFile, smallFile);
        assertThrows(OcrException.class, () -> submissionService.submitImages(sixFiles, assignment.getId(), student));

        // 3. Single photo > 10MB
        MockMultipartFile largeSingle = new MockMultipartFile("files", "huge.png", "image/png", new byte[11 * 1024 * 1024]);
        assertThrows(OcrException.class, () -> submissionService.submitImages(List.of(largeSingle), assignment.getId(), student));

        // 4. OCR returns blank
        when(ocrService.extractText(any())).thenReturn("   ");
        assertThrows(OcrException.class, () -> submissionService.submitImages(List.of(smallFile), assignment.getId(), student));

        // 5. Successful multi-page submission
        MockMultipartFile page1 = new MockMultipartFile("files", "p1.png", "image/png", new byte[]{10, 20});
        MockMultipartFile page2 = new MockMultipartFile("files", "p2.png", "image/png", new byte[]{30, 40});

        when(ocrService.extractText(eq(new byte[]{10, 20}))).thenReturn("Page one answers");
        when(ocrService.extractText(eq(new byte[]{30, 40}))).thenReturn("Page two answers");

        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(scoringService.scoreGrammarTask(anyString(), anyString(), eq(student)))
            .thenReturn(Map.of("score", 90.0, "feedback", "Great job"));
        when(scoringService.extractScore(any())).thenReturn(90.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = submissionService.submitImages(List.of(page1, page2), assignment.getId(), student);
        assertNotNull(res);
        assertEquals(90.0, res.getScore());
        verify(usageService).incrementOcr(student, 2);
    }

    /**
     * @brief Verifies unit test scenario: advanced diagnostic edge cases, score override, and answer variants.
     */
    @Test
    void testEnrichSubmissionResultAdvancedScenarios() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Reported Speech")
            .build();

        // Q1: Target Grammar Structure fallback (rule is blank, task has grammarTopic)
        TaskQuestion q1 = TaskQuestion.builder()
            .questionOrder(1)
            .questionText("Question 1")
            .correctAnswer("don't / do not")
            .grammarRule("")
            .optionsJson("[\"do not\", \"does not\"]")
            .points(1)
            .build();

        // Q2: Variant matching with comma, Rule A (1 mistake out of 1 attempted -> 100% error rate)
        TaskQuestion q2 = TaskQuestion.builder()
            .questionOrder(2)
            .questionText("Question 2")
            .correctAnswer("color, colour")
            .grammarRule("Spelling Rule A")
            .optionsJson("not valid json{")
            .points(1)
            .build();

        // Q3: Rule B (1 mistake out of 2 attempted -> 50% error rate)
        TaskQuestion q3 = TaskQuestion.builder()
            .questionOrder(3)
            .questionText("Question 3")
            .correctAnswer("were")
            .grammarRule("Past Subjunctive B")
            .points(1)
            .build();

        // Q4: Rule B second attempt (correct)
        TaskQuestion q4 = TaskQuestion.builder()
            .questionOrder(4)
            .questionText("Question 4")
            .correctAnswer("had been")
            .grammarRule("Past Subjunctive B")
            .points(1)
            .build();

        // Q5: Rule C (100% correct, 0 incorrect, 0 skipped -> Mastered Strength!)
        TaskQuestion q5 = TaskQuestion.builder()
            .questionOrder(5)
            .questionText("Question 5")
            .correctAnswer("apple")
            .grammarRule("Articles Mastered")
            .points(1)
            .build();

        task.setQuestions(List.of(q1, q2, q3, q4, q5));

        TaskAssignment assign = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .build();

        // Student answers:
        // Q1: "do not" matches variant "don't / do not" -> correct!
        // Q2: "wrong-spelling" -> incorrect!
        // Q3: "wrong-verb" -> incorrect!
        // Q4: "had been" -> correct!
        // Q5: "apple" -> correct!
        String studentText = "Q1: do not\nQ2: wrong-spelling\nQ3: wrong-verb\nQ4: had been\nQ5: apple";

        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(assign)
            .student(student)
            .studentText(studentText)
            .aiScore(60.0)
            .overrideScore(95.0) // Teacher override
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(sub);
        submissionService.enrichSubmissionResult(resp, sub, null);

        // Effective score should be teacher override
        assertEquals(95.0, resp.getEffectiveScore());
        assertEquals(60.0, resp.getScore());

        assertNotNull(resp.getAiAnalysis());
        var analysis = resp.getAiAnalysis();

        // Mastered concept Articles Mastered must be in strengths!
        assertTrue(analysis.getStrengths().contains("Articles Mastered"));

        // Spelling Rule A (1 error / 1 attempt = 100% error rate) should rank before Past Subjunctive B (1 error / 2 attempts = 50% error rate)
        int idxA = analysis.getWeaknesses().indexOf("Spelling Rule A");
        int idxB = analysis.getWeaknesses().indexOf("Past Subjunctive B");
        assertTrue(idxA >= 0);
        assertTrue(idxB >= 0);
        assertTrue(idxA < idxB, "Rule A with higher error rate should come before Rule B");

        // Verify fallback when ruleStats is empty
        Submission emptySub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("Free text with no questions")
            .aiScore(80.0)
            .build();
        SubmissionResultResponse emptyResp = SubmissionResultResponse.fromEntity(emptySub);
        submissionService.enrichSubmissionResult(emptyResp, emptySub, null);
        assertNotNull(emptyResp.getAiAnalysis());
    }

    /**
     * @brief Verifies objective score calculates and persists when objective items are evaluated.
     */
    @Test
    void testEnrichSubmissionResult_PersistsObjectiveScoreWhenAiScoreEvaluated() {
        Task t = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Conditionals")
            .questions(List.of(
                TaskQuestion.builder().questionOrder(1).questionText("Q1").correctAnswer("apple").points(1).grammarRule("Conditionals").build(),
                TaskQuestion.builder().questionOrder(2).questionText("Q2").correctAnswer("banana").points(1).grammarRule("Conditionals").build()
            ))
            .build();

        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("1. apple\n2. orange")
            .aiScore(90.0)
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(sub);
        resp.setTaskId(t.getId());
        when(taskRepository.findById(t.getId())).thenReturn(Optional.of(t));

        submissionService.enrichSubmissionResult(resp, sub, null);

        assertEquals(50.0, resp.getScore());
        assertEquals(50.0, resp.getEffectiveScore());
        assertEquals(50.0, sub.getAiScore());
        verify(submissionRepository).save(sub);
    }

    /**
     * @brief Verifies student preferredProvider, oversized images, answer alternatives, and CAT options enrichment.
     */
    @Test
    void testSubmissionService_ComprehensiveAdditionalCoverage() {
        // 1. Student with preferred provider
        student.setPreferredProvider(com.linguaoptima.api.domain.enums.AIProvider.GEMINI);
        Map<String, Object> essayResult = Map.of("score", 85.0);
        when(scoringService.scoreEssay(anyString(), anyString(), eq(student))).thenReturn(essayResult);
        when(scoringService.extractScore(essayResult)).thenReturn(85.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        TextSubmissionRequest textReq = TextSubmissionRequest.builder()
            .text("Essay with preferred provider")
            .type("ESSAY")
            .build();
        SubmissionResultResponse textRes = submissionService.submitText(textReq, student);
        assertNotNull(textRes);
        assertEquals("AI_GEMINI", textRes.getProviderUsed());

        // 2. Oversized image files > 25MB
        org.springframework.mock.web.MockMultipartFile hugeFile = new org.springframework.mock.web.MockMultipartFile(
            "file", "huge.jpg", "image/jpeg", new byte[26 * 1024 * 1024]
        );
        assertThrows(com.linguaoptima.api.exception.OcrException.class,
            () -> submissionService.submitImages(List.of(hugeFile), null, student));

        // 3. submitImage single file with null file delegation
        assertThrows(Exception.class, () -> submissionService.submitImage(null, null, student));

        // 4. Answer evaluation with 'or' alternative answers (e.g., 'theater or theatre')
        Task taskOr = Task.builder()
            .id(UUID.randomUUID())
            .questions(List.of(
                TaskQuestion.builder().questionOrder(1).questionText("Spell theatre").correctAnswer("theater or theatre").points(1).build()
            ))
            .build();
        Submission subOr = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("1. theatre")
            .build();
        SubmissionResultResponse respOr = SubmissionResultResponse.fromEntity(subOr);
        respOr.setTaskId(taskOr.getId());
        when(taskRepository.findById(taskOr.getId())).thenReturn(Optional.of(taskOr));

        submissionService.enrichSubmissionResult(respOr, subOr, null);
        assertNotNull(respOr.getItems());
        assertEquals(1, respOr.getItems().size());
        assertTrue(respOr.getItems().get(0).isCorrect());

        // 5. CAT answers JSON with optionsJson and skipped question
        String catAnswers = "[{\"questionId\":\"" + UUID.randomUUID() + "\",\"questionText\":\"Q1\",\"correctAnswer\":\"correct\",\"givenAnswer\":\"\",\"isCorrect\":false,\"grammarRule\":\"Mixed Conditionals\",\"optionsJson\":\"[\\\"A\\\",\\\"B\\\"]\"}]";
        TaskAssignment catAssign = TaskAssignment.builder().id(UUID.randomUUID()).student(student).build();
        Submission catSub = Submission.builder()
            .id(UUID.randomUUID())
            .assignment(catAssign)
            .student(student)
            .studentText("CAT session")
            .aiScore(40.0)
            .build();
        SessionState catSession = SessionState.builder()
            .id(UUID.randomUUID())
            .assignment(catAssign)
            .answersJson(catAnswers)
            .build();
        when(sessionStateRepository.findByAssignmentId(catAssign.getId())).thenReturn(Optional.of(catSession));
        SubmissionResultResponse catResp = SubmissionResultResponse.fromEntity(catSub);
        submissionService.enrichSubmissionResult(catResp, catSub, null);
        assertNotNull(catResp.getItems());
        assertEquals(1, catResp.getItems().size());
        assertTrue(catResp.getItems().get(0).isSkipped());
        assertNotNull(catResp.getItems().get(0).getOptions());

        // 6. Save exception catch branch in enrichSubmissionResult
        doThrow(new RuntimeException("DB down")).when(submissionRepository).save(any(Submission.class));
        submissionService.enrichSubmissionResult(respOr, subOr, null);
    }

    /**
     * @brief Verifies teacher override persists structured corrections and item adjustments into aiFeedback JSON.
     */
    @Test
    void testOverrideScoreWithCorrectionsAndItems() {
        UUID subId = UUID.randomUUID();
        Submission sub = Submission.builder()
            .id(subId)
            .student(student)
            .aiScore(70.0)
            .aiFeedback("{\"feedback\":\"Initial evaluation\",\"corrections\":[],\"items\":[]}")
            .studentText("1. She have two cats.")
            .build();

        SentenceCorrectionResponse corr = SentenceCorrectionResponse.builder()
            .original("She have")
            .corrected("She has")
            .explanation("Subject-verb agreement: third-person singular requires 'has'")
            .grammarRule("Present Simple")
            .build();

        SubmissionItemResponse item = SubmissionItemResponse.builder()
            .questionNumber(1)
            .sentence("She ___ two cats.")
            .studentAnswer("have")
            .correctAnswer("has")
            .isCorrect(false)
            .isSkipped(false)
            .explanation("Incorrect auxiliary/verb agreement")
            .grammarRule("Present Simple")
            .points(10)
            .options(List.of("have", "has"))
            .build();

        OverrideRequest req = OverrideRequest.builder()
            .overrideScore(85.0)
            .teacherComment("Well analyzed by teacher")
            .feedback("Teacher updated AI commentary")
            .corrections(List.of(corr))
            .items(List.of(item))
            .build();

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(subId, req, teacher);
        assertNotNull(res);
        assertEquals(85.0, res.getOverrideScore());
        assertEquals("Teacher updated AI commentary", res.getFeedback());
        assertTrue(sub.getAiFeedback().contains("She have"));
        assertTrue(sub.getAiFeedback().contains("Present Simple"));

        // Retrieve and verify items and corrections are restored from serialized JSON
        when(submissionRepository.findById(subId)).thenReturn(Optional.of(sub));
        SubmissionResultResponse retrieved = submissionService.getSubmissionById(subId, teacher);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getCorrections());
        assertEquals(1, retrieved.getCorrections().size());
        assertEquals("She have", retrieved.getCorrections().get(0).getOriginal());
        assertNotNull(retrieved.getItems());
        assertEquals(1, retrieved.getItems().size());
        assertEquals("have", retrieved.getItems().get(0).getStudentAnswer());
        assertNotNull(retrieved.getItems().get(0).getOptions());
    }

    /**
     * @brief Verifies overrideScore handles plain-text aiFeedback without throwing exceptions.
     */
    @Test
    void testOverrideScoreWithPlainTextFeedback() {
        UUID subId = UUID.randomUUID();
        Submission sub = Submission.builder()
            .id(subId)
            .student(student)
            .aiScore(65.0)
            .aiFeedback("Plain text assessment without JSON formatting")
            .build();

        OverrideRequest req = OverrideRequest.builder()
            .overrideScore(75.0)
            .feedback("Updated summary notes")
            .build();

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResultResponse res = submissionService.overrideScore(subId, req, teacher);
        assertNotNull(res);
        assertEquals(75.0, res.getOverrideScore());
        assertTrue(sub.getAiFeedback().contains("Updated summary notes"));
    }

    /**
     * @brief Verifies that open-ended questions use semantic AI evaluation rather than strict string equality.
     */
    @Test
    void testEnrichFromTaskQuestionsSemanticEvaluation() {
        Task openTask = Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.READING)
            .cefrLevel(CefrLevel.B2)
            .questions(List.of(
                TaskQuestion.builder()
                    .id(UUID.randomUUID())
                    .questionOrder(1)
                    .questionText("What causes the reduction in energy loss?")
                    .correctAnswer("aerodynamic efficiency")
                    .points(15)
                    .difficulty(2)
                    .grammarRule("Reading: Open Question")
                    .build()
            ))
            .build();

        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .studentText("1. streamlined air resistance and optimized aerodynamic flow")
            .aiScore(100.0)
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(sub);
        resp.setTaskId(openTask.getId());
        when(taskRepository.findById(openTask.getId())).thenReturn(Optional.of(openTask));

        // AI rubric marked question 1 as correct despite non-exact string match
        Map<String, Object> rubric = Map.of(
            "items", List.of(
                Map.of(
                    "questionNumber", 1,
                    "isCorrect", true,
                    "explanation", "Semantically valid: student accurately described aerodynamic flow.",
                    "grammarRule", "Reading: Open Question"
                )
            )
        );

        submissionService.enrichSubmissionResult(resp, sub, rubric);
        assertNotNull(resp.getItems());
        assertEquals(1, resp.getItems().size());
        assertTrue(resp.getItems().get(0).isCorrect());
        assertEquals(15, resp.getItems().get(0).getPoints());
        assertTrue(resp.getItems().get(0).getExplanation().contains("Semantically valid"));

        // Now test when rubric is absent but aiFeedback JSON has the evaluation item
        sub.setAiFeedback("""
            {
              "items": [
                {
                  "questionNumber": 1,
                  "isCorrect": true,
                  "explanation": "Semantically correct from stored feedback",
                  "grammarRule": "Reading: Open Question"
                }
              ]
            }
            """);
        SubmissionResultResponse respFromJson = SubmissionResultResponse.fromEntity(sub);
        respFromJson.setTaskId(openTask.getId());
        submissionService.enrichSubmissionResult(respFromJson, sub, null);
        assertNotNull(respFromJson.getItems());
        assertEquals(1, respFromJson.getItems().size());
        assertTrue(respFromJson.getItems().get(0).isCorrect());
        assertTrue(respFromJson.getItems().get(0).getExplanation().contains("Semantically correct from stored feedback"));
    }

    /**
     * @brief Verifies unit test scenario: submit speaking practice task routes to scoreSpeakingTask.
     */
    @Test
    void testSubmitSpeakingPracticeTask() {
        Task speakingTask = Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Pronunciation & Fluency")
            .content("Consistent pronunciation practice enhances phonetic awareness.")
            .answerKey("Consistent pronunciation practice enhances phonetic awareness.")
            .build();

        when(taskRepository.findById(speakingTask.getId())).thenReturn(Optional.of(speakingTask));
        when(taskAssignmentRepository.findByStudentIdAndTaskId(eq(student.getId()), eq(speakingTask.getId()))).thenReturn(Optional.empty());
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> speakingRubric = Map.of(
            "score", 94.0,
            "overallScore", 94.0,
            "accuracyScore", 96.0,
            "fluencyScore", 92.0,
            "speechRateWpm", 130,
            "pacingScore", 98.0,
            "feedback", "Speaking evaluation completed: 94% overall score.",
            "items", List.of(
                Map.of(
                    "questionNumber", 1,
                    "isCorrect", true,
                    "explanation", "Pronunciation Accuracy: 96% | Speech Rate: 130 WPM | Pacing: 98%",
                    "grammarRule", "Oral Speech Cadence & Phonetic Articulation"
                )
            )
        );
        when(scoringService.scoreSpeakingTask(anyString(), anyString(), eq(student))).thenReturn(speakingRubric);
        when(scoringService.extractScore(speakingRubric)).thenReturn(94.0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        String payload = """
            [SPEAKING_PRACTICE]
            Overall Score: 94%
            Pronunciation Accuracy: 96%
            Speech Rate: 130 WPM

            Spoken Transcript: Consistent pronunciation practice enhances phonetic awareness.

            Reference Passage: Consistent pronunciation practice enhances phonetic awareness.
            """;

        TextSubmissionRequest req = TextSubmissionRequest.builder()
            .taskId(speakingTask.getId())
            .text(payload)
            .type("SPEAKING")
            .build();

        SubmissionResultResponse resp = submissionService.submitText(req, student);
        assertNotNull(resp);
        assertEquals(94.0, resp.getScore());
        verify(scoringService).scoreSpeakingTask(anyString(), anyString(), eq(student));
    }

    /**
     * @brief Verifies unit test scenario: enrich speaking task submission preserves score and extracts transcript.
     */
    @Test
    void testEnrichSubmissionResultSpeakingTask() {
        Task speakingTask = Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.SPEAKING)
            .cefrLevel(CefrLevel.B1)
            .grammarTopic("Pronunciation & Fluency")
            .questions(List.of(
                TaskQuestion.builder()
                    .id(UUID.randomUUID())
                    .questionOrder(1)
                    .questionText("Read aloud")
                    .correctAnswer("Target passage")
                    .points(20)
                    .build()
            ))
            .build();

        String payload = """
            [SPEAKING_PRACTICE]
            Overall Score: 95%
            Speech Rate: 125 WPM

            Spoken Transcript: Target passage spoken clearly.

            Reference Passage: Target passage
            """;

        Submission sub = Submission.builder()
            .id(UUID.randomUUID())
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .studentText(payload)
            .aiScore(95.0)
            .aiFeedback("""
                {
                  "items": [
                    {
                      "questionNumber": 1,
                      "isCorrect": true,
                      "explanation": "Pronunciation Accuracy: 96% | Speech Rate: 125 WPM",
                      "grammarRule": "Oral Articulation"
                    }
                  ]
                }
                """)
            .build();

        SubmissionResultResponse resp = SubmissionResultResponse.fromEntity(sub);
        resp.setTaskId(speakingTask.getId());
        when(taskRepository.findById(speakingTask.getId())).thenReturn(Optional.of(speakingTask));

        submissionService.enrichSubmissionResult(resp, sub, null);
        assertNotNull(resp.getItems());
        assertEquals(1, resp.getItems().size());
        assertEquals("Target passage spoken clearly.", resp.getItems().get(0).getStudentAnswer());
        assertEquals(95.0, resp.getScore());
        assertEquals(95.0, resp.getEffectiveScore());

        // Test with teacher override score present
        sub.setOverrideScore(99.0);
        SubmissionResultResponse respOverride = SubmissionResultResponse.fromEntity(sub);
        respOverride.setTaskId(speakingTask.getId());
        submissionService.enrichSubmissionResult(respOverride, sub, null);
        assertEquals(95.0, respOverride.getScore());
        assertEquals(99.0, respOverride.getEffectiveScore());
    }
}

