/**
 * @file TransferTaskRequest.java
 * @brief Request DTO for transferring or copying an assigned task between study cohorts.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Request DTO for transferring or copying an assigned task between study cohorts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTaskRequest {

    /** @brief Target group ID to transfer the task to. */
    @NotNull(message = "Target group ID is required")
    private UUID targetGroupId;

    /** @brief If true, retains assignment in source group (copy); if false, removes pending assignments from source group (move). */
    @Builder.Default
    private boolean keepInSource = false;

    /** @brief Optional new submission due date for the target cohort. If null, retains source due date. */
    private LocalDateTime dueDate;

    /** @brief Optional new maximum attempts for the target cohort. If null, retains source max attempts. */
    private Integer maxAttempts;
}
