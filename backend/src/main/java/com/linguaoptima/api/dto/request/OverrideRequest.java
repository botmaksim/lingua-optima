package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file OverrideRequest.java
 * @brief Request DTO for teacher manual score override and comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideRequest {

    @NotNull(message = "Override score is required")
    private Double overrideScore;

    private String teacherComment;
}
