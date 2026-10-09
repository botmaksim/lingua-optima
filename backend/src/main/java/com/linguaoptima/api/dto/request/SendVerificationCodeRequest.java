/**
 * @file SendVerificationCodeRequest.java
 * @brief Request DTO for sending an email verification code for registration.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for sending an email verification code for registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationCodeRequest {

    /** @brief Target email address for receiving verification code. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
