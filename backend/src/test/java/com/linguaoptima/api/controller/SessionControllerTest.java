/**
 * @file SessionControllerTest.java
 * @brief Unit and slice test suite for SessionController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AnswerRequest;
import com.linguaoptima.api.dto.response.AnswerFeedbackResponse;
import com.linguaoptima.api.dto.response.QuestionResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for SessionController.
 */
@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    /** @brief Test fixture or mock dependency for session service. */
    @Mock
    private SessionService sessionService;

    /** @brief Test fixture or mock dependency for session controller. */
    @InjectMocks
    private SessionController sessionController;

    /** @brief Test fixture or mock dependency for student. */
    private User student;
    /** @brief Test fixture or mock dependency for session state. */
    private SessionState sessionState;
    /** @brief Test fixture or mock dependency for session id. */
    private UUID sessionId;

    /**
     * @brief Initializes test fixtures and mock state before each test in SessionControllerTest.
     */
    @BeforeEach
    void setUp() {
        student = User.builder().id(UUID.randomUUID()).build();
        sessionId = UUID.randomUUID();
        sessionState = SessionState.builder().id(sessionId).build();
    }

    /**
     * @brief Verifies unit test scenario: start session.
     */
    @Test
    void testStartSession() {
        UUID assignmentId = UUID.randomUUID();
        when(sessionService.startSession(assignmentId, student)).thenReturn(sessionState);

        ResponseEntity<SessionState> res = sessionController.startSession(
            Map.of("assignmentId", assignmentId.toString()), student);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        assertThrows(IllegalArgumentException.class, () -> sessionController.startSession(Map.of(), student));
    }

    /**
     * @brief Verifies unit test scenario: get active session.
     */
    @Test
    void testGetActiveSession() {
        when(sessionService.getActiveSession(student)).thenReturn(Optional.of(sessionState));
        assertEquals(HttpStatus.OK, sessionController.getActiveSession(student).getStatusCode());

        when(sessionService.getActiveSession(student)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NO_CONTENT, sessionController.getActiveSession(student).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: get next question and answer and complete.
     */
    @Test
    void testGetNextQuestionAndAnswerAndComplete() {
        QuestionResponse qr = QuestionResponse.builder().id(UUID.randomUUID()).build();
        when(sessionService.getNextQuestion(sessionId, student)).thenReturn(qr);
        assertEquals(HttpStatus.OK, sessionController.getNextQuestion(sessionId, student).getStatusCode());

        AnswerRequest answerReq = AnswerRequest.builder().questionId(qr.getId()).answer("A").build();
        AnswerFeedbackResponse afr = AnswerFeedbackResponse.builder().correct(true).build();
        when(sessionService.submitAnswer(sessionId, answerReq, student)).thenReturn(afr);
        assertEquals(HttpStatus.OK, sessionController.submitAnswer(sessionId, answerReq, student).getStatusCode());

        SubmissionResultResponse srr = SubmissionResultResponse.builder().id(UUID.randomUUID()).build();
        when(sessionService.completeSession(sessionId, student)).thenReturn(srr);
        assertEquals(HttpStatus.OK, sessionController.completeSession(sessionId, student).getStatusCode());
    }
}
