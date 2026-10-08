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

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    private User student;
    private SessionState sessionState;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        student = User.builder().id(UUID.randomUUID()).build();
        sessionId = UUID.randomUUID();
        sessionState = SessionState.builder().id(sessionId).build();
    }

    @Test
    void testStartSession() {
        UUID assignmentId = UUID.randomUUID();
        when(sessionService.startSession(assignmentId, student)).thenReturn(sessionState);

        ResponseEntity<SessionState> res = sessionController.startSession(
            Map.of("assignmentId", assignmentId.toString()), student);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        assertThrows(IllegalArgumentException.class, () -> sessionController.startSession(Map.of(), student));
    }

    @Test
    void testGetActiveSession() {
        when(sessionService.getActiveSession(student)).thenReturn(Optional.of(sessionState));
        assertEquals(HttpStatus.OK, sessionController.getActiveSession(student).getStatusCode());

        when(sessionService.getActiveSession(student)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NO_CONTENT, sessionController.getActiveSession(student).getStatusCode());
    }

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
