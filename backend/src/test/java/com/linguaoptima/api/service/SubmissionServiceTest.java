/**
 * @file SubmissionServiceTest.java
 * @brief Unit and slice test suite for SubmissionService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.TaskQuestion;
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
            .build();

        com.linguaoptima.api.domain.TaskQuestion q2 = com.linguaoptima.api.domain.TaskQuestion.builder()
            .questionOrder(2)
            .questionText("She ___ (go) home.")
            .correctAnswer("went / had gone")
            .grammarRule("Past Simple Auxiliary Usage")
            .optionsJson("invalid-json")
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
        assertTrue(respCustom.getItems().get(0).getExplanation().contains("Correct! Accurately applies the rule: Grammar"));
        assertTrue(respCustom.getItems().get(2).getExplanation().contains("Incorrect. The expected answer is 'omega'"));

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
}
