/**
 * @file CurriculumControllerTest.java
 * @brief Unit tests for CurriculumController verifying upload and reference endpoints.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.CurriculumFileType;
import com.linguaoptima.api.dto.response.CurriculumReferenceResponse;
import com.linguaoptima.api.dto.response.CurriculumUploadResponse;
import com.linguaoptima.api.service.CurriculumStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @brief Unit tests for CurriculumController.
 */
@ExtendWith(MockitoExtension.class)
class CurriculumControllerTest {

    @Mock
    private CurriculumStorageService curriculumStorageService;

    @InjectMocks
    private CurriculumController curriculumController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(UUID.randomUUID()).email("user@lingua.com").build();
    }

    @Test
    void testUploadCurriculumFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "rule.md",
            "text/markdown",
            "Grammar rule content".getBytes(StandardCharsets.UTF_8)
        );

        CurriculumUploadResponse uploadResp = CurriculumUploadResponse.builder()
            .fileName("rule.md")
            .fileType(CurriculumFileType.RULE)
            .fileSize(file.getSize())
            .contentSnippet("Grammar rule snippet")
            .fullContent("Grammar rule content")
            .serverPath("custom/rules/rule.md")
            .topic("Conditionals")
            .build();

        when(curriculumStorageService.saveCurriculumFile(file, CurriculumFileType.RULE, "Conditionals", testUser))
            .thenReturn(uploadResp);

        ResponseEntity<CurriculumUploadResponse> response = curriculumController.uploadCurriculumFile(
            file,
            CurriculumFileType.RULE,
            "Conditionals",
            testUser
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("rule.md", response.getBody().getFileName());
        assertEquals(CurriculumFileType.RULE, response.getBody().getFileType());
    }

    @Test
    void testGetReferenceCurriculum() {
        CurriculumReferenceResponse refResp = CurriculumReferenceResponse.builder()
            .cefrLevel("B2")
            .grammarTopic("Conditionals")
            .referenceRule("If + past, would...")
            .referenceVocabulary(List.of("hypothetical"))
            .source("CANONICAL")
            .build();

        when(curriculumStorageService.getReferenceCurriculum(CefrLevel.B2, "Conditionals"))
            .thenReturn(refResp);

        ResponseEntity<CurriculumReferenceResponse> response = curriculumController.getReferenceCurriculum(
            CefrLevel.B2,
            "Conditionals"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CANONICAL", response.getBody().getSource());
        assertEquals("B2", response.getBody().getCefrLevel());
    }
}
