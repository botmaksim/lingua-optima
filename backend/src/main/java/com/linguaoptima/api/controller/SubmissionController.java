package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/text")
    public ResponseEntity<SubmissionResultResponse> submitText(
        @Valid @RequestBody TextSubmissionRequest request,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(submissionService.submitText(request, student));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResultResponse> submitImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "assignmentId", required = false) UUID assignmentId,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(submissionService.submitImage(file, assignmentId, student));
    }

    @GetMapping("/my")
    public ResponseEntity<List<SubmissionResultResponse>> getMySubmissions(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(submissionService.getMySubmissions(student));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResultResponse> getSubmission(
        @PathVariable("id") UUID submissionId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(submissionService.getSubmissionById(submissionId, user));
    }

    @PutMapping("/{id}/override")
    public ResponseEntity<SubmissionResultResponse> overrideScore(
        @PathVariable("id") UUID submissionId,
        @Valid @RequestBody OverrideRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(submissionService.overrideScore(submissionId, request, teacher));
    }
}
