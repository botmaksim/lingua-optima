/**
 * @file ForgotPasswordRequest.java
 * @brief Request DTO for initiating a password reset flow.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for initiating a password reset flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /** @brief Field representing email in ForgotPasswordRequest. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
