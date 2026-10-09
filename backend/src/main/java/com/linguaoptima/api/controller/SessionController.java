/**
 * @file SessionController.java
 * @brief REST controller driving adaptive Computerized Adaptive Testing (CAT) sessions.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AnswerRequest;
import com.linguaoptima.api.dto.response.AnswerFeedbackResponse;
import com.linguaoptima.api.dto.response.QuestionResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief REST controller driving adaptive Computerized Adaptive Testing (CAT) sessions.
 *
 * Manages dynamic difficulty adjustments, in-flight question requests, and CAT test completions.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    /** @brief Field representing session service in SessionController. */
    private final SessionService sessionService;

    /**
     * @brief Initializes a new adaptive testing session for a student assignment.
     *
     * @param body Payload mapping containing assignmentId.
     * @param student Authenticated student principal.
     * @return HTTP 200 with newly created SessionState entity.
     */
    @PostMapping("/start")
    public ResponseEntity<SessionState> startSession(
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal User student
    ) {
        String assignmentIdStr = body.get("assignmentId");
        if (assignmentIdStr == null || assignmentIdStr.isBlank()) {
            throw new IllegalArgumentException("assignmentId is required");
        }
        UUID assignmentId = UUID.fromString(assignmentIdStr);
        return ResponseEntity.ok(sessionService.startSession(assignmentId, student));
    }

    /**
     * @brief Checks for any currently in-progress active testing session for resuming.
     *
     * @param student Authenticated student principal.
     * @return HTTP 200 with active SessionState, or 204 No Content.
     */
    @GetMapping("/active")
    public ResponseEntity<SessionState> getActiveSession(@AuthenticationPrincipal User student) {
        Optional<SessionState> active = sessionService.getActiveSession(student);
        return active.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * @brief Generates or fetches next adaptively calibrated question in the session.
     *
     * @param sessionId Identifier of active session.
     * @param student Authenticated student principal.
     * @return HTTP 200 with QuestionResponse containing question options.
     */
    @GetMapping("/{id}/next-question")
    public ResponseEntity<QuestionResponse> getNextQuestion(
        @PathVariable("id") UUID sessionId,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.getNextQuestion(sessionId, student));
    }

    /**
     * @brief Processes student answer and returns instantaneous grading feedback.
     *
     * @param sessionId Identifier of active session.
     * @param request Payload containing question identifier and selected answer.
     * @param student Authenticated student principal.
     * @return HTTP 200 with AnswerFeedbackResponse detailing correctness and explanations.
     */
    @PostMapping("/{id}/answer")
    public ResponseEntity<AnswerFeedbackResponse> submitAnswer(
        @PathVariable("id") UUID sessionId,
        @Valid @RequestBody AnswerRequest request,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.submitAnswer(sessionId, request, student));
    }

    /**
     * @brief Concludes the adaptive session and finalizes cumulative CEFR scoring.
     *
     * @param sessionId Identifier of active session to complete.
     * @param student Authenticated student principal.
     * @return HTTP 200 with final SubmissionResultResponse.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<SubmissionResultResponse> completeSession(
        @PathVariable("id") UUID sessionId,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.completeSession(sessionId, student));
    }
}
