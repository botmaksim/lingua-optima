/**
 * @file UpdateStudentNameRequest.java
 * @brief Request DTO for educators to rename or alias a student account.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for educators to rename or alias a student account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentNameRequest {

    /**
     * @brief New full name or custom identifier for the student account.
     */
    @NotBlank(message = "Student name cannot be blank")
    @Size(min = 1, max = 150, message = "Student name must be between 1 and 150 characters")
    private String fullName;
}
