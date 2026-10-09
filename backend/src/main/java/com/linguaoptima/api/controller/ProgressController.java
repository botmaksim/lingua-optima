package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.ProgressResponse;
import com.linguaoptima.api.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @file ProgressController.java
 * @brief REST controller for tracking CEFR mastery, grammar gaps, and advancement level-ups.
 *
 * Exposes endpoints for student self-analytics, educator diagnostics, and CEFR level promotion.
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * @brief Fetches grammar topic mastery breakdown for authenticated student.
     *
     * @param student Authenticated student principal.
     * @return HTTP 200 with list of progress metrics across CEFR topics.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ProgressResponse>> getMyProgress(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(progressService.getProgressForStudent(student.getId()));
    }

    /**
     * @brief Retrieves student progress history for educator review.
     *
     * @param studentId Unique identifier of student.
     * @return HTTP 200 with list of progress records.
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<List<ProgressResponse>> getStudentProgress(@PathVariable("id") UUID studentId) {
        return ResponseEntity.ok(progressService.getProgressForStudent(studentId));
    }

    /**
     * @brief Aggregates progress metrics across all students in a cohort group.
     *
     * @param groupId Unique identifier of group.
     * @return HTTP 200 with aggregated topic mastery scores.
     */
    @GetMapping("/group/{id}")
    public ResponseEntity<List<ProgressResponse>> getGroupProgress(@PathVariable("id") UUID groupId) {
        return ResponseEntity.ok(progressService.getGroupProgress(groupId));
    }

    /**
     * @brief Confirms promotion to the next suggested CEFR proficiency level.
     *
     * @param student Authenticated student principal.
     * @return HTTP 200 OK after CEFR level update.
     */
    @PostMapping("/level-up")
    public ResponseEntity<Void> confirmLevelUp(@AuthenticationPrincipal User student) {
        progressService.confirmLevelUp(student);
        return ResponseEntity.ok().build();
    }
}
