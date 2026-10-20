/**
 * @file ProgressControllerTest.java
 * @brief Unit and slice test suite for ProgressController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.ProgressResponse;
import com.linguaoptima.api.service.ProgressService;
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
 * @brief Unit and slice test suite for ProgressController.
 */
@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private ProgressController progressController;

    private User student;
    private ProgressResponse progressResponse;

    @BeforeEach
    void setUp() {
        student = User.builder().id(UUID.randomUUID()).build();
        progressResponse = ProgressResponse.builder().grammarTopic("Passive").masteryScore(0.85).build();
    }

    /**
     * @brief Verifies unit test scenario: get progress me.
     */
    @Test
    void testGetProgressMe() {
        when(progressService.getProgressForStudent(student.getId())).thenReturn(List.of(progressResponse));
        when(progressService.getGapsForStudent(student.getId())).thenReturn(List.of(progressResponse));
        ResponseEntity<List<ProgressResponse>> res = progressController.getMyProgress(student);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        assertEquals(HttpStatus.OK, progressController.getMyGaps(student).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: get student and group progress.
     */
    @Test
    void testGetStudentAndGroupProgress() {
        UUID studentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(progressService.getProgressForStudent(studentId)).thenReturn(List.of(progressResponse));
        when(progressService.getGroupProgress(groupId)).thenReturn(List.of(progressResponse));

        assertEquals(HttpStatus.OK, progressController.getStudentProgress(studentId).getStatusCode());
        assertEquals(HttpStatus.OK, progressController.getGroupProgress(groupId).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: confirm level up.
     */
    @Test
    void testConfirmLevelUp() {
        when(progressService.confirmLevelUp(student)).thenReturn(student);
        ResponseEntity<User> res = progressController.confirmLevelUp(student);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(student, res.getBody());
        verify(progressService).confirmLevelUp(student);
    }
}
