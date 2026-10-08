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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @Mock
    private SessionStateRepository sessionStateRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private TaskQuestionRepository taskQuestionRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ProgressService progressService;
    @Mock
    private GamificationService gamificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SessionService sessionService;

    private User student;
    private Task task;
    private TaskAssignment assignment;
    private SessionState sessionState;

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

    @Test
    void testStartSessionSuccess() {
        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(sessionStateRepository.save(any(SessionState.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionState state = sessionService.startSession(assignment.getId(), student);
        assertNotNull(state);
        assertEquals(2, state.getCurrentDifficulty());
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment.getStatus());
    }

    @Test
    void testStartSessionOtherStudentThrows() {
        User other = User.builder().id(UUID.randomUUID()).build();
        when(taskAssignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        assertThrows(ForbiddenException.class, () -> sessionService.startSession(assignment.getId(), other));
    }

    @Test
    void testGetActiveSession() {
        when(sessionStateRepository.findFirstByStudentIdAndStatusOrderByStartedAtDesc(student.getId(), SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.of(sessionState));

        Optional<SessionState> active = sessionService.getActiveSession(student);
        assertTrue(active.isPresent());
        assertEquals(sessionState.getId(), active.get().getId());
    }

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
        assertEquals(3, res.getNewDifficulty()); // 2 -> 3
        assertEquals(1, res.getCurrentQuestionIndex());
        assertFalse(res.isCompleted());
        verify(progressService).updateFromSubmission(student, "Past Simple", true);
    }

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
        assertEquals(1, res.getNewDifficulty()); // 2 -> 1
        verify(progressService).updateFromSubmission(student, "Past Simple", false);
    }

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

    @Test
    void testSessionNotFoundThrows() {
        UUID unknown = UUID.randomUUID();
        when(sessionStateRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getNextQuestion(unknown, student));
    }

    @Test
    void testGetNextQuestionNoQuestionsThrows() {
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findByTaskIdAndDifficulty(task.getId(), 2)).thenReturn(List.of());
        when(taskQuestionRepository.findByTaskIdOrderByQuestionOrder(task.getId())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getNextQuestion(sessionState.getId(), student));
    }

    @Test
    void testSubmitAnswerQuestionNotFoundThrows() {
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        UUID unknownQ = UUID.randomUUID();
        when(taskQuestionRepository.findById(unknownQ)).thenReturn(Optional.empty());

        AnswerRequest req = AnswerRequest.builder().questionId(unknownQ).answer("ans").build();
        assertThrows(ResourceNotFoundException.class, () -> sessionService.submitAnswer(sessionState.getId(), req, student));
    }

    @Test
    void testSubmitAnswerClampsDifficultyBoundaries() {
        sessionState.setCurrentDifficulty(4);
        TaskQuestion tq = TaskQuestion.builder().id(UUID.randomUUID()).correctAnswer("ans").difficulty(4).grammarRule("Rule").build();
        when(sessionStateRepository.findById(sessionState.getId())).thenReturn(Optional.of(sessionState));
        when(taskQuestionRepository.findById(tq.getId())).thenReturn(Optional.of(tq));

        AnswerRequest req = AnswerRequest.builder().questionId(tq.getId()).answer("ans").build();
        AnswerFeedbackResponse res = sessionService.submitAnswer(sessionState.getId(), req, student);
        assertEquals(4, res.getNewDifficulty()); // Clamped at 4 max

        sessionState.setCurrentDifficulty(1);
        req.setAnswer("wrong");
        AnswerFeedbackResponse res2 = sessionService.submitAnswer(sessionState.getId(), req, student);
        assertEquals(1, res2.getNewDifficulty()); // Clamped at 1 min
    }
}
