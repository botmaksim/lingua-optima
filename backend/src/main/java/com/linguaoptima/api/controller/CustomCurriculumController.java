/**
 * @file CustomCurriculumController.java
 * @brief REST controller for teacher custom grammar rules and vocabulary sets.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.CustomCurriculumRequest;
import com.linguaoptima.api.dto.response.CustomCurriculumResponse;
import com.linguaoptima.api.service.CustomCurriculumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller for teacher custom grammar rules and vocabulary sets.
 */
@RestController
@RequestMapping("/api/curriculum/custom")
@RequiredArgsConstructor
public class CustomCurriculumController {

    private final CustomCurriculumService customCurriculumService;

    /**
     * @brief Lists custom curriculum entries saved by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<CustomCurriculumResponse>> getCustomCurriculum(
        @RequestParam(value = "type", required = false) String type,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(customCurriculumService.getCustomCurriculum(user, type));
    }

    /**
     * @brief Saves a new custom rule or vocabulary dictionary.
     */
    @PostMapping
    public ResponseEntity<CustomCurriculumResponse> createCustomCurriculum(
        @Valid @RequestBody CustomCurriculumRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(customCurriculumService.createCustomCurriculum(request, user));
    }

    /**
     * @brief Deletes a saved custom curriculum entry.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomCurriculum(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal User user
    ) {
        customCurriculumService.deleteCustomCurriculum(id, user);
        return ResponseEntity.noContent().build();
    }
}
