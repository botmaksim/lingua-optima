/**
 * @file CreateGroupRequest.java
 * @brief Request DTO for creating a new student cohort group.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for creating a new student cohort group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {

    /** @brief Field representing name in CreateGroupRequest. */
    @NotBlank(message = "Group name is required")
    private String name;
}
