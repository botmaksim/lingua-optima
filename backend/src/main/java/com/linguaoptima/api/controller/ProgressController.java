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

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/me")
    public ResponseEntity<List<ProgressResponse>> getMyProgress(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(progressService.getProgressForStudent(student.getId()));
    }

    @GetMapping("/student/{id}")
    public ResponseEntity<List<ProgressResponse>> getStudentProgress(@PathVariable("id") UUID studentId) {
        return ResponseEntity.ok(progressService.getProgressForStudent(studentId));
    }

    @GetMapping("/group/{id}")
    public ResponseEntity<List<ProgressResponse>> getGroupProgress(@PathVariable("id") UUID groupId) {
        return ResponseEntity.ok(progressService.getGroupProgress(groupId));
    }

    @PostMapping("/level-up")
    public ResponseEntity<Void> confirmLevelUp(@AuthenticationPrincipal User student) {
        progressService.confirmLevelUp(student);
        return ResponseEntity.ok().build();
    }
}
