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

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

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

    @GetMapping("/active")
    public ResponseEntity<SessionState> getActiveSession(@AuthenticationPrincipal User student) {
        Optional<SessionState> active = sessionService.getActiveSession(student);
        return active.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/next-question")
    public ResponseEntity<QuestionResponse> getNextQuestion(
        @PathVariable("id") UUID sessionId,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.getNextQuestion(sessionId, student));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<AnswerFeedbackResponse> submitAnswer(
        @PathVariable("id") UUID sessionId,
        @Valid @RequestBody AnswerRequest request,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.submitAnswer(sessionId, request, student));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SubmissionResultResponse> completeSession(
        @PathVariable("id") UUID sessionId,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(sessionService.completeSession(sessionId, student));
    }
}
