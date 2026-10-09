/**
 * @file RegisterRequest.java
 * @brief Request DTO for new student or teacher account registration.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for new student or teacher account registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** @brief Field representing email in RegisterRequest. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** @brief Field representing password in RegisterRequest. */
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /** @brief Field representing full name in RegisterRequest. */
    @NotBlank(message = "Full name is required")
    private String fullName;

    /** @brief Field representing role in RegisterRequest. */
    @NotNull(message = "Role is required")
    private Role role;

    /** @brief Optional initial CEFR proficiency level (defaults to A1). */
    private CefrLevel cefrLevel;

    /** @brief 6-digit email verification code sent to the registration email. */
    private String verificationCode;
}
