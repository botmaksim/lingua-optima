/**
 * @file ProgressResponse.java
 * @brief Response DTO representing student mastery metrics for a grammar topic.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linguaoptima.api.domain.ProgressRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Response DTO representing student mastery metrics for a grammar topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {

    /** @brief Unique identifier of the progress record. */
    private UUID id;

    /** @brief Curriculum grammar topic name. */
    private String grammarTopic;

    /** @brief Total number of attempts recorded for this topic. */
    private int totalAttempts;

    /** @brief Number of incorrect attempts recorded for this topic. */
    private int errorCount;

    /** @brief Computed mastery ratio between 0.0 and 1.0. */
    private double masteryScore;

    /** @brief Timestamp of the latest practice update. */
    private LocalDateTime updatedAt;

    /**
     * @brief Exposes updatedAt as lastPracticedAt for frontend compatibility.
     * @return Timestamp of the latest practice update.
     */
    @JsonProperty("lastPracticedAt")
    public LocalDateTime getLastPracticedAt() {
        return updatedAt;
    }

    /**
     * @brief Maps a domain ProgressRecord entity to a ProgressResponse DTO.
     * @param record Domain entity instance.
     * @return Mapped ProgressResponse DTO or null if input is null.
     */
    public static ProgressResponse fromEntity(ProgressRecord record) {
        if (record == null) return null;
        return ProgressResponse.builder()
            .id(record.getId())
            .grammarTopic(record.getGrammarTopic())
            .totalAttempts(record.getTotalAttempts())
            .errorCount(record.getErrorCount())
            .masteryScore(record.getMasteryScore())
            .updatedAt(record.getUpdatedAt())
            .build();
    }
}
