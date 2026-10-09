/**
 * @file SessionServiceTest.java
 * @brief Unit and slice test suite for SessionService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SessionStatus;
import com.linguaoptima.api.domain.enums.TaskType;
import com.linguaoptima.api.dto.request.AnswerRequest;
import com.linguaoptima.api.dto.response.AnswerFeedbackResponse;
import com.linguaoptima.api.dto.response.QuestionResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
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
 * @brief Unit and slice test suite for SessionService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    /** @brief Test fixture or mock dependency for session state repository. */
    @Mock
    private SessionStateRepository sessionStateRepository;
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Test fixture or mock dependency for task question repository. */
    @Mock
    private TaskQuestionRepository taskQuestionRepository;
    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Test fixture or mock dependency for progress service. */
    @Mock
    private ProgressService progressService;
    /** @brief Test fixture or mock dependency for gamification service. */
    @Mock
    private GamificationService gamificationService;

    /** @brief Test fixture or mock dependency for object mapper. */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    /** @brief Test fixture or mock dependency for session service. */
    @InjectMocks
    private SessionService sessionService;

    /** @brief Test fixture or mock dependency for student. */
    private User student;
    /** @brief Test fixture or mock dependency for task. */
    private Task task;
    /** @brief Test fixture or mock dependency for assignment. */
    private TaskAssignment assignment;
    /** @brief Test fixture or mock dependency for session state. */
    private SessionState sessionState;

    /**
     * @brief Initializes test fixtures and mock state before each test in SessionServiceTest.
     */
    @BeforeEach
    void setUp() {
        student = User.builder()
            .id(UUID.randomUUID())
            .email("cat@lingua.com")
            .role(Role.STUDENT)
            .build();

        task = Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MCQ)
            .build();

        assignment = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .status(AssignmentStatus.PENDING)
            .build();

        sessionState = SessionState.builder()
            .id(UUID.randomUUID())
            .assignment(assignment)
            .student(student)
            .currentDifficulty(2)
            .currentQuestionIndex(0)
            .answersJson("[]")
            .status(SessionStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();
    }

    /**
     * @brief Verifies unit test scenario: start session success.
     */
    @Test
    void testStartSessionSuccess() {
        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(sessionStateRepository.save(any(SessionState.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionState state = sessionService.startSession(assignment.getId(), student);
        assertNotNull(state);
        assertEquals(2, state.getCurrentDifficulty());
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment.getStatus());
    }

    /**
     * @brief Verifies unit test scenario: start session other student throws.
     */
    @Test
    void testStartSessionOtherStudentThrows() {
        User other = User.builder().id(UUID.randomUUID()).build();
        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        assertThrows(ForbiddenException.class, () -> sessionService.startSession(assignment.getId(), other));
    }

    /**
     * @brief Verifies unit test scenario: get active session.
     */
    @Test
    void testGetActiveSession() {
        when(sessionStateRepository.findFirstByStudentIdAndStatusOrderByStartedAtDesc(student.getId(), SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.of(sessionState));

        Optional<SessionState> active = sessionService.getActiveSession(student);
        assertTrue(active.isPresent());
        assertEquals(sessionState.getId(), active.get().getId());
    }

    /**
     * @brief Verifies unit test scenario: get next question.
     */
    @Test
    void testGetNextQuestion() {
        TaskQuestion tq = TaskQuestion.builder()
            .id(UUID.randomUUID())
            .task(task)
            .questionText("Sample question?")
            .correctAnswer("Ans")
            .difficulty(2)
            .build();

        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findByTaskIdAndDifficulty(task.getId(), 2)).thenReturn(List.of(tq));

        QuestionResponse qResp = sessionService.getNextQuestion(sessionState.getId(), student);
        assertNotNull(qResp);
        assertEquals("Sample question?", qResp.getQuestionText());
    }

    /**
     * @brief Verifies unit test scenario: submit answer correct difficulty increases.
     */
    @Test
    void testSubmitAnswerCorrectDifficultyIncreases() {
        TaskQuestion tq = TaskQuestion.builder()
            .id(UUID.randomUUID())
            .task(task)
            .correctAnswer("correct_opt")
            .difficulty(2)
            .grammarRule("Past Simple")
            .build();

        AnswerRequest req = AnswerRequest.builder()
            .questionId(tq.getId())
            .answer("correct_opt")
            .build();

        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findById(tq.getId())).thenReturn(Optional.of(tq));

        AnswerFeedbackResponse res = sessionService.submitAnswer(sessionState.getId(), req, student);

        assertTrue(res.isCorrect());
        assertEquals(3, res.getNewDifficulty());
        assertEquals(1, res.getCurrentQuestionIndex());
        assertFalse(res.isCompleted());
        verify(progressService).updateFromSubmission(student, "Past Simple", true);
    }

    /**
     * @brief Verifies unit test scenario: submit answer wrong difficulty decreases.
     */
    @Test
    void testSubmitAnswerWrongDifficultyDecreases() {
        TaskQuestion tq = TaskQuestion.builder()
            .id(UUID.randomUUID())
            .task(task)
            .correctAnswer("correct_opt")
            .difficulty(2)
            .grammarRule("Past Simple")
            .build();

        AnswerRequest req = AnswerRequest.builder()
            .questionId(tq.getId())
            .answer("wrong_opt")
            .build();

        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findById(tq.getId())).thenReturn(Optional.of(tq));

        AnswerFeedbackResponse res = sessionService.submitAnswer(sessionState.getId(), req, student);

        assertFalse(res.isCorrect());
        assertEquals(1, res.getNewDifficulty());
        verify(progressService).updateFromSubmission(student, "Past Simple", false);
    }

    /**
     * @brief Verifies unit test scenario: submit10th answer completes session.
     */
    @Test
    void testSubmit10thAnswerCompletesSession() {
        sessionState.setCurrentQuestionIndex(9);
        TaskQuestion tq = TaskQuestion.builder()
            .id(UUID.randomUUID())
            .task(task)
            .correctAnswer("ans")
            .difficulty(4)
            .grammarRule("Conditionals")
            .build();

        AnswerRequest req = AnswerRequest.builder().questionId(tq.getId()).answer("ans").build();

        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findById(tq.getId())).thenReturn(Optional.of(tq));

        AnswerFeedbackResponse res = sessionService.submitAnswer(sessionState.getId(), req, student);

        assertTrue(res.isCompleted());
        assertEquals(SessionStatus.COMPLETED, sessionState.getStatus());
        verify(submissionRepository).save(any(Submission.class));
        verify(gamificationService).onSubmissionCompleted(student);
    }

    /**
     * @brief Verifies unit test scenario: complete session.
     */
    @Test
    void testCompleteSession() {
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(submissionRepository.findByAssignmentId(assignment.getId())).thenReturn(List.of());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResultResponse res = sessionService.completeSession(sessionState.getId(), student);
        assertNotNull(res);
        assertEquals(SessionStatus.COMPLETED, sessionState.getStatus());
        verify(gamificationService).onSubmissionCompleted(student);
    }

    /**
     * @brief Verifies unit test scenario: session not found throws.
     */
    @Test
    void testSessionNotFoundThrows() {
        UUID unknown = UUID.randomUUID();
        when(sessionStateRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getNextQuestion(unknown, student));
    }

    /**
     * @brief Verifies unit test scenario: get next question no questions throws.
     */
    @Test
    void testGetNextQuestionNoQuestionsThrows() {
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findByTaskIdAndDifficulty(task.getId(), 2)).thenReturn(List.of());
        when(taskQuestionRepository.findByTaskIdOrderByQuestionOrder(task.getId())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getNextQuestion(sessionState.getId(), student));
    }

    /**
     * @brief Verifies unit test scenario: submit answer question not found throws.
     */
    @Test
    void testSubmitAnswerQuestionNotFoundThrows() {
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        UUID unknownQ = UUID.randomUUID();
        when(taskQuestionRepository.findById(unknownQ)).thenReturn(Optional.empty());

        AnswerRequest req = AnswerRequest.builder().questionId(unknownQ).answer("ans").build();
        assertThrows(ResourceNotFoundException.class, () -> sessionService.submitAnswer(sessionState.getId(), req, student));
    }

    /**
     * @brief Verifies unit test scenario: submit answer clamps difficulty boundaries.
     */
    @Test
    void testSubmitAnswerClampsDifficultyBoundaries() {
        sessionState.setCurrentDifficulty(4);
        TaskQuestion tq = TaskQuestion.builder().id(UUID.randomUUID()).correctAnswer("ans").difficulty(4).grammarRule("Rule").build();
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findById(tq.getId())).thenReturn(Optional.of(tq));

        AnswerRequest req = AnswerRequest.builder().questionId(tq.getId()).answer("ans").build();
        AnswerFeedbackResponse res = sessionService.submitAnswer(sessionState.getId(), req, student);
        assertEquals(4, res.getNewDifficulty());

        sessionState.setCurrentDifficulty(1);
        req.setAnswer("wrong");
        AnswerFeedbackResponse res2 = sessionService.submitAnswer(sessionState.getId(), req, student);
        assertEquals(1, res2.getNewDifficulty());
    }
}
