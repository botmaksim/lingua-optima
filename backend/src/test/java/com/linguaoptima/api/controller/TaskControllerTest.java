/**
 * @file TaskControllerTest.java
 * @brief Unit and slice test suite for TaskController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.request.AssignTaskRequest;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.TaskResponse;
import com.linguaoptima.api.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for TaskController.
 */
@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private User user;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        taskResponse = TaskResponse.builder().id(UUID.randomUUID()).content("Task content").build();
    }

    /**
     * @brief Verifies unit test scenario: get tasks and get by id.
     */
    @Test
    void testGetTasksAndGetById() {
        when(taskService.getTasksForUser(user)).thenReturn(List.of(taskResponse));
        when(taskService.getTaskById(taskResponse.getId(), user)).thenReturn(taskResponse);

        assertEquals(HttpStatus.OK, taskController.getTasks(user).getStatusCode());
        assertEquals(HttpStatus.OK, taskController.getTaskById(taskResponse.getId(), user).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: generate and preview task.
     */
    @Test
    void testGenerateAndPreviewTask() {
        TaskParamsRequest req = TaskParamsRequest.builder().build();
        when(taskService.generateTask(req, user, "test-device")).thenReturn(taskResponse);
        when(taskService.previewTask(req, user, "test-device")).thenReturn(taskResponse);

        assertEquals(HttpStatus.OK, taskController.generateTask(req, "test-device", user).getStatusCode());
        assertEquals(HttpStatus.OK, taskController.previewTask(req, "test-device", user).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: template and assign task.
     */
    @Test
    void testTemplateAndAssignTask() {
        TaskParamsRequest req = TaskParamsRequest.builder().build();
        when(taskService.saveAsTemplate(req, user)).thenReturn(taskResponse);

        assertEquals(HttpStatus.OK, taskController.saveAsTemplate(req, user).getStatusCode());

        AssignTaskRequest assignReq = AssignTaskRequest.builder().build();
        ResponseEntity<Void> assignRes = taskController.assignTask(taskResponse.getId(), assignReq, user);
        assertEquals(HttpStatus.OK, assignRes.getStatusCode());
        verify(taskService).assignTask(taskResponse.getId(), assignReq, user);
    }

    /**
     * @brief Verifies unit test scenario: get topics catalog.
     */
    @Test
    void testGetTopicsCatalog() {
        com.linguaoptima.api.dto.response.TopicsCatalogResponse catalog =
            com.linguaoptima.api.dto.response.TopicsCatalogResponse.builder().build();
        when(taskService.getTopicsCatalog()).thenReturn(catalog);

        ResponseEntity<com.linguaoptima.api.dto.response.TopicsCatalogResponse> res = taskController.getTopicsCatalog();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(catalog, res.getBody());
    }

    /**
     * @brief Verifies unit test scenario: create custom task.
     */
    @Test
    void testCreateCustomTask() {
        com.linguaoptima.api.dto.request.CreateCustomTaskRequest req =
            com.linguaoptima.api.dto.request.CreateCustomTaskRequest.builder().build();
        when(taskService.createCustomTask(req, user)).thenReturn(taskResponse);

        ResponseEntity<com.linguaoptima.api.dto.response.TaskResponse> res = taskController.createCustomTask(req, user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(taskResponse, res.getBody());
    }

    /**
     * @brief Verifies unit test scenario: takeover generation.
     */
    @Test
    void testTakeoverGeneration() {
        ResponseEntity<java.util.Map<String, Object>> res = taskController.takeoverGeneration("device-123", user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(true, res.getBody().get("success"));
        verify(taskService).takeoverGeneration(user, "device-123");
    }

    /**
     * @brief Verifies unit test scenario: get generation status.
     */
    @Test
    void testGetGenerationStatus() {
        com.linguaoptima.api.dto.response.GenerationStatusResponse statusResp =
            com.linguaoptima.api.dto.response.GenerationStatusResponse.builder()
                .isGenerating(true)
                .activeDeviceId("device-123")
                .isCurrentDevice(true)
                .build();
        when(taskService.getGenerationStatus(user, "device-123")).thenReturn(statusResp);

        ResponseEntity<com.linguaoptima.api.dto.response.GenerationStatusResponse> res =
            taskController.getGenerationStatus("device-123", user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(statusResp, res.getBody());
    }

    /**
     * @brief Verifies unit test scenario: explain sentence.
     */
    @Test
    void testExplainSentence() {
        com.linguaoptima.api.dto.request.ExplainSentenceRequest req =
            com.linguaoptima.api.dto.request.ExplainSentenceRequest.builder()
                .sentence("The passive voice was used.")
                .cefrLevel(CefrLevel.B1)
                .build();

        com.linguaoptima.api.dto.response.ExplainSentenceResponse resp =
            com.linguaoptima.api.dto.response.ExplainSentenceResponse.builder()
                .originalSentence("The passive voice was used.")
                .translation("Пассивный залог был использован.")
                .grammarBreakdown("Past Simple Passive")
                .keyVocabulary(List.of("used — использованный"))
                .explanation("Страдательный залог.")
                .build();

        when(taskService.explainSentence(req, user)).thenReturn(resp);

        ResponseEntity<com.linguaoptima.api.dto.response.ExplainSentenceResponse> res =
            taskController.explainSentence(req, user);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(resp, res.getBody());
    }
}
