/**
 * @file SubmissionControllerTest.java
 * @brief Unit and slice test suite for SubmissionController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for SubmissionController.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    /** @brief Test fixture or mock dependency for submission service. */
    @Mock
    private SubmissionService submissionService;

    /** @brief Test fixture or mock dependency for submission controller. */
    @InjectMocks
    private SubmissionController submissionController;

    /** @brief Test fixture or mock dependency for user. */
    private User user;
    /** @brief Test fixture or mock dependency for result response. */
    private SubmissionResultResponse resultResponse;

    /**
     * @brief Initializes test fixtures and mock state before each test in SubmissionControllerTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        resultResponse = SubmissionResultResponse.builder().id(UUID.randomUUID()).score(90.0).build();
    }

    /**
     * @brief Verifies unit test scenario: submit text and image.
     */
    @Test
    void testSubmitTextAndImage() {
        TextSubmissionRequest req = TextSubmissionRequest.builder().text("Text").build();
        when(submissionService.submitText(req, user)).thenReturn(resultResponse);
        assertEquals(HttpStatus.OK, submissionController.submitText(req, user).getStatusCode());

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2});
        when(submissionService.submitImage(file, null, user)).thenReturn(resultResponse);
        assertEquals(HttpStatus.OK, submissionController.submitImage(file, null, user).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: get my submissions and get by id.
     */
    @Test
    void testGetMySubmissionsAndGetById() {
        UUID groupId = UUID.randomUUID();
        when(submissionService.getMySubmissions(user)).thenReturn(List.of(resultResponse));
        when(submissionService.getGroupSubmissions(groupId, user)).thenReturn(List.of(resultResponse));
        when(submissionService.getSubmissionById(resultResponse.getId(), user)).thenReturn(resultResponse);

        assertEquals(HttpStatus.OK, submissionController.getMySubmissions(user).getStatusCode());
        assertEquals(HttpStatus.OK, submissionController.getGroupSubmissions(groupId, user).getStatusCode());
        assertEquals(HttpStatus.OK, submissionController.getSubmission(resultResponse.getId(), user).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: override score.
     */
    @Test
    void testOverrideScore() {
        OverrideRequest req = OverrideRequest.builder().overrideScore(95.0).build();
        when(submissionService.overrideScore(resultResponse.getId(), req, user)).thenReturn(resultResponse);

        ResponseEntity<SubmissionResultResponse> res = submissionController.overrideScore(resultResponse.getId(), req, user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
