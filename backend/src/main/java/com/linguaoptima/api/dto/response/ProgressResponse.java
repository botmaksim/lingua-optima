package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.ProgressRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
