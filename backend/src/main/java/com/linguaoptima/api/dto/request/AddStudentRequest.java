/**
 * @file AddStudentRequest.java
 * @brief Request DTO for enrolling a student into a cohort group by email.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for enrolling a student into a cohort group by email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentRequest {

    /** @brief Field representing email in AddStudentRequest. */
    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String email;
}
