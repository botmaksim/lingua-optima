/**
 * @file AssignTaskRequest.java
 * @brief Request DTO for assigning a generated task to one or more student groups.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @brief Request DTO for assigning a generated task to one or more student groups.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTaskRequest {

    /** @brief Field representing group ids in AssignTaskRequest. */
    @NotEmpty(message = "At least one group ID must be provided")
    private List<UUID> groupIds;

    /** @brief Field representing due date in AssignTaskRequest. */
    private LocalDateTime dueDate;
}
