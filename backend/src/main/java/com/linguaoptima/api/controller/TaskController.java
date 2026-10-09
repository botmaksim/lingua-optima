/**
 * @file TaskController.java
 * @brief REST controller for AI task generation, catalog previews, and cohort assignments.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.dto.response.TopicsCatalogResponse;
import com.linguaoptima.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller for AI task generation, catalog previews, and cohort assignments.
 *
 * Implements self-service practice task creation and educator assignment distribution.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    /** @brief Field representing task service in TaskController. */
    private final TaskService taskService;

    /**
     * @brief Retrieves canonical CEFR syllabus topics, dedicated mixed challenges, and common domains.
     *
     * @return HTTP 200 with TopicsCatalogResponse.
     */
    @GetMapping("/topics")
    public ResponseEntity<TopicsCatalogResponse> getTopicsCatalog() {
        return ResponseEntity.ok(taskService.getTopicsCatalog());
    }

    /**
     * @brief Lists available tasks and assignments assigned to the user.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with list of TaskResponse items.
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getTasksForUser(user));
    }

    /**
     * @brief Fetches full task metadata and questions by unique identifier.
     *
     * @param taskId Unique identifier of the task.
     * @return HTTP 200 with TaskResponse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable("id") UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    /**
     * @brief Generates and persists a customized practice task using AI pipeline.
     *
     * @param request Generation parameters (CEFR level, topic, domain, task type).
     * @param user Authenticated user principal.
     * @return HTTP 200 with generated TaskResponse.
     */
    @PostMapping("/generate")
    public ResponseEntity<TaskResponse> generateTask(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(taskService.generateTask(request, user));
    }

    /**
     * @brief Generates an ephemeral preview of a task without persisting to database.
     *
     * @param request Generation parameters.
     * @param user Authenticated user principal.
     * @return HTTP 200 with preview TaskResponse.
     */
    @PostMapping("/preview")
    public ResponseEntity<TaskResponse> previewTask(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(taskService.previewTask(request, user));
    }

    /**
     * @brief Saves a configured task configuration as a curriculum template.
     *
     * @param request Generation parameters.
     * @param teacher Authenticated educator principal.
     * @return HTTP 200 with saved template TaskResponse.
     */
    @PostMapping("/template")
    public ResponseEntity<TaskResponse> saveAsTemplate(
        @Valid @RequestBody TaskParamsRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(taskService.saveAsTemplate(request, teacher));
    }

    /**
     * @brief Deploys and assigns an existing task to designated student cohorts.
     *
     * @param taskId Identifier of task to assign.
     * @param request Assignment payload containing groupIds and due dates.
     * @param teacher Authenticated educator principal.
     * @return HTTP 200 OK.
     */
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
