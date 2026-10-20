/**
 * @file OverrideRequest.java
 * @brief Request DTO for teacher manual score override and comment.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.dto.response.SentenceCorrectionResponse;
import com.linguaoptima.api.dto.response.SubmissionItemResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
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

    /** @brief Optional revised AI diagnostic feedback / evaluation comment. */
    private String feedback;

    /** @brief Optional teacher-updated sentence corrections and highlighted grammar mistakes. */
    private List<SentenceCorrectionResponse> corrections;

    /** @brief Optional teacher-updated question items and diagnostic explanations. */
    private List<SubmissionItemResponse> items;
}
