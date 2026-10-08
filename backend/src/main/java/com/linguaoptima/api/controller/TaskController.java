package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getTasksForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable("id") UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @PostMapping("/generate")
    public ResponseEntity<TaskResponse> generateTask(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(taskService.generateTask(request, user));
    }

    @PostMapping("/preview")
    public ResponseEntity<TaskResponse> previewTask(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(taskService.previewTask(request, user));
    }

    @PostMapping("/template")
    public ResponseEntity<TaskResponse> saveAsTemplate(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(taskService.saveAsTemplate(request, teacher));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Void> assignTask(
        @PathVariable("id") UUID taskId,
        @Valid @RequestBody AssignTaskRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        taskService.assignTask(taskId, request, teacher);
        return ResponseEntity.ok().build();
    }
}
