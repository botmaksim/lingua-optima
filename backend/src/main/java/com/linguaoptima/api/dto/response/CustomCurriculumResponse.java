/**
 * @file CustomCurriculumResponse.java
 * @brief Response DTO representing a saved educator custom rule or vocabulary dictionary.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.CustomCurriculumEntry;
import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Response DTO representing a saved educator custom rule or vocabulary dictionary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomCurriculumResponse {

    /** @brief Unique identifier of the entry. */
    private UUID id;

    /** @brief Title or topic name. */
    private String title;

    /** @brief Associated CEFR level. */
    private CefrLevel cefrLevel;

    /** @brief Entry classification: RULE or VOCABULARY. */
    private String curriculumType;

    /** @brief Content string (markdown rule or vocabulary list). */
    private String content;

    /** @brief Creation timestamp. */
    private LocalDateTime createdAt;

    /**
     * @brief Maps a JPA entity to this response DTO.
     */
    public static CustomCurriculumResponse fromEntity(CustomCurriculumEntry entity) {
        if (entity == null) return null;
        return CustomCurriculumResponse.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .cefrLevel(entity.getCefrLevel())
            .curriculumType(entity.getCurriculumType())
            .content(entity.getContent())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
