/**
 * @file TaskControllerTest.java
 * @brief Unit and slice test suite for TaskController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
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

    /** @brief Test fixture or mock dependency for task service. */
    @Mock
    private TaskService taskService;

    /** @brief Test fixture or mock dependency for task controller. */
    @InjectMocks
    private TaskController taskController;

    /** @brief Test fixture or mock dependency for user. */
    private User user;
    /** @brief Test fixture or mock dependency for task response. */
    private TaskResponse taskResponse;

    /**
     * @brief Initializes test fixtures and mock state before each test in TaskControllerTest.
     */
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
        when(taskService.getTaskById(taskResponse.getId())).thenReturn(taskResponse);

        assertEquals(HttpStatus.OK, taskController.getTasks(user).getStatusCode());
        assertEquals(HttpStatus.OK, taskController.getTaskById(taskResponse.getId()).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: generate and preview task.
     */
    @Test
    void testGenerateAndPreviewTask() {
        TaskParamsRequest req = TaskParamsRequest.builder().build();
        when(taskService.generateTask(req, user)).thenReturn(taskResponse);
        when(taskService.previewTask(req, user)).thenReturn(taskResponse);

        assertEquals(HttpStatus.OK, taskController.generateTask(req, user).getStatusCode());
        assertEquals(HttpStatus.OK, taskController.previewTask(req, user).getStatusCode());
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
}
