/**
 * @file CurriculumController.java
 * @brief REST controller for uploading custom grammar rules, vocabulary lists, and retrieving curriculum references.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.CurriculumFileType;
import com.linguaoptima.api.dto.response.CurriculumReferenceResponse;
import com.linguaoptima.api.dto.response.CurriculumUploadResponse;
import com.linguaoptima.api.service.CurriculumStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @brief REST endpoints for curriculum file uploads and pedagogical reference queries.
 */
@RestController
@RequestMapping("/api/tasks/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    /** @brief Service managing curriculum file persistence and reference resolution. */
    private final CurriculumStorageService curriculumStorageService;

    /**
     * @brief Uploads custom grammar rules or vocabulary reference materials to server storage.
     * @param file Uploaded text, markdown, or JSON file.
     * @param type Discriminator indicating rule or vocabulary material.
     * @param topic Optional topic associated with the custom curriculum file.
     * @param user Authenticated user initiating upload.
     * @return HTTP 200 with CurriculumUploadResponse containing extracted snippet and server path.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurriculumUploadResponse> uploadCurriculumFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") CurriculumFileType type,
        @RequestParam(value = "topic", required = false) String topic,
        @AuthenticationPrincipal User user
    ) {
        CurriculumUploadResponse response = curriculumStorageService.saveCurriculumFile(file, type, topic, user);
        return ResponseEntity.ok(response);
    }

    /**
     * @brief Retrieves canonical or synthesized pedagogical reference materials for a given CEFR level and topic.
     * @param level Target CEFR proficiency benchmark.
     * @param topic Grammar topic or custom practice subject.
     * @return HTTP 200 with CurriculumReferenceResponse.
     */
    @GetMapping("/reference")
    public ResponseEntity<CurriculumReferenceResponse> getReferenceCurriculum(
        @RequestParam(value = "level", required = false, defaultValue = "B1") CefrLevel level,
        @RequestParam(value = "topic", required = false, defaultValue = "General") String topic
    ) {
        CurriculumReferenceResponse response = curriculumStorageService.getReferenceCurriculum(level, topic);
        return ResponseEntity.ok(response);
    }
}
