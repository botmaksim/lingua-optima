/**
 * @file SubmissionController.java
 * @brief REST controller handling student work submissions (text and Zero-Retention OCR).
 */
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller handling student work submissions (text and Zero-Retention OCR).
 *
 * Implements essay rubric evaluations, RAM-only OCR processing, and teacher grade overrides.
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    /** @brief Field representing submission service in SubmissionController. */
    private final SubmissionService submissionService;

    /**
     * @brief Submits written text or essay for automated AI rubric evaluation.
     *
     * @param request Payload containing essay text and optional assignment ID.
     * @param student Authenticated student principal.
     * @return HTTP 200 with rubric scoring and lexical feedback.
     */
    @PostMapping("/text")
    public ResponseEntity<SubmissionResultResponse> submitText(
        @Valid @RequestBody TextSubmissionRequest request,
        @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(submissionService.submitText(request, student));
    }

    /**
     * @brief Ingests handwritten homework image for Zero-Retention OCR and AI feedback.
     *
     * @param file Uploaded image multipart file (processed purely in RAM).
     * @param assignmentId Optional assignment link.
     * @param student Authenticated student principal.
     * @return HTTP 200 with extracted text and rubric assessment.
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResultResponse> submitImage(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "files", required = false) List<MultipartFile> files,
        @RequestParam(value = "assignmentId", required = false) UUID assignmentId,
        @AuthenticationPrincipal User student
    ) {
        List<MultipartFile> allFiles = new ArrayList<>();
        if (files != null) {
            allFiles.addAll(files);
        }
        if (file != null && !allFiles.contains(file)) {
            allFiles.add(file);
        }
        return ResponseEntity.ok(submissionService.submitImages(allFiles, assignmentId, student));
    }

    /**
     * @brief Fetches submission history for authenticated student or educator.
     *
     * @param student Authenticated user principal.
     * @return HTTP 200 with list of submission results.
     */
    @GetMapping({"/my", "/me"})
    public ResponseEntity<List<SubmissionResultResponse>> getMySubmissions(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(submissionService.getMySubmissions(student));
    }

    /**
     * @brief Fetches all submissions for a specific cohort group owned by the educator.
     *
     * @param groupId Unique identifier of the group.
     * @param teacher Authenticated educator principal.
     * @return HTTP 200 with list of group submission results.
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<SubmissionResultResponse>> getGroupSubmissions(
        @PathVariable("groupId") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(submissionService.getGroupSubmissions(groupId, teacher));
    }

    /**
     * @brief Fetches all student submissions for groups and assignments belonging to the educator.
     *
     * Ensures educator self-practice submissions are excluded from the review queue.
     *
     * @param teacher Authenticated educator principal.
     * @return HTTP 200 with list of student submissions for educator review.
     */
    @GetMapping("/teacher")
    public ResponseEntity<List<SubmissionResultResponse>> getTeacherSubmissions(
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(submissionService.getTeacherSubmissions(teacher));
    }

    /**
     * @brief Retrieves submission evaluation details by ID.
     *
     * @param submissionId Unique identifier of submission.
     * @param user Authenticated user principal.
     * @return HTTP 200 with SubmissionResultResponse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResultResponse> getSubmission(
        @PathVariable("id") UUID submissionId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(submissionService.getSubmissionById(submissionId, user));
    }

    /**
     * @brief Allows educators to override AI-assigned score and leave feedback notes.
     *
     * @param submissionId Unique identifier of submission.
     * @param request Payload containing override score and commentary.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 200 with updated SubmissionResultResponse.
     */
    @RequestMapping(value = "/{id}/override", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<SubmissionResultResponse> overrideScore(
        @PathVariable("id") UUID submissionId,
        @Valid @RequestBody OverrideRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(submissionService.overrideScore(submissionId, request, teacher));
    }
}
