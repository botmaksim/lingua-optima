package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.ProgressRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @file ProgressResponse.java
 * @brief Response DTO representing student mastery metrics for a grammar topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {

    private String grammarTopic;
    private int totalAttempts;
    private int errorCount;
    private double masteryScore;
    private LocalDateTime updatedAt;

    /**
     * @brief Maps a domain ProgressRecord entity to a ProgressResponse DTO.
     * @param record Domain entity instance.
     * @return Mapped ProgressResponse DTO or null if input is null.
     */
    public static ProgressResponse fromEntity(ProgressRecord record) {
        if (record == null) return null;
        return ProgressResponse.builder()
            .grammarTopic(record.getGrammarTopic())
            .totalAttempts(record.getTotalAttempts())
            .errorCount(record.getErrorCount())
            .masteryScore(record.getMasteryScore())
            .updatedAt(record.getUpdatedAt())
            .build();
    }
}
