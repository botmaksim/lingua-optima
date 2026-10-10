/**
 * @file CustomCurriculumControllerTest.java
 * @brief Unit tests for CustomCurriculumController REST endpoints.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.CustomCurriculumRequest;
import com.linguaoptima.api.dto.response.CustomCurriculumResponse;
import com.linguaoptima.api.service.CustomCurriculumService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomCurriculumControllerTest {

    @Mock
    private CustomCurriculumService customCurriculumService;

    @InjectMocks
    private CustomCurriculumController customCurriculumController;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@lingua.com")
            .role(Role.TEACHER)
            .build();
    }

    @Test
    void testGetCustomCurriculum() {
        CustomCurriculumResponse resp = CustomCurriculumResponse.builder()
            .id(UUID.randomUUID())
            .title("Rule 1")
            .curriculumType("RULE")
            .build();

        when(customCurriculumService.getCustomCurriculum(teacher, "RULE")).thenReturn(List.of(resp));

        ResponseEntity<List<CustomCurriculumResponse>> response =
            customCurriculumController.getCustomCurriculum("RULE", teacher);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Rule 1", response.getBody().get(0).getTitle());
    }

    @Test
    void testCreateCustomCurriculum() {
        CustomCurriculumRequest req = CustomCurriculumRequest.builder()
            .title("New Vocab")
            .cefrLevel(CefrLevel.B2)
            .curriculumType("VOCABULARY")
            .content("apple, banana")
            .build();

        CustomCurriculumResponse resp = CustomCurriculumResponse.builder()
            .id(UUID.randomUUID())
            .title("New Vocab")
            .curriculumType("VOCABULARY")
            .build();

        when(customCurriculumService.createCustomCurriculum(req, teacher)).thenReturn(resp);

        ResponseEntity<CustomCurriculumResponse> response =
            customCurriculumController.createCustomCurriculum(req, teacher);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("New Vocab", response.getBody().getTitle());
    }

    @Test
    void testDeleteCustomCurriculum() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = customCurriculumController.deleteCustomCurriculum(id, teacher);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(customCurriculumService).deleteCustomCurriculum(id, teacher);
    }
}
