/**
 * @file OverrideRequest.java
 * @brief Request DTO for teacher manual score override and comment.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for teacher manual score override and comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideRequest {

    /** @brief Field representing override score in OverrideRequest. */
    @NotNull(message = "Override score is required")
    private Double overrideScore;

    /** @brief Field representing teacher comment in OverrideRequest. */
    private String teacherComment;
}
