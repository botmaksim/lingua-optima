/**
 * @file LoginRequest.java
 * @brief Request DTO for user email and password authentication.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for user email and password authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** @brief Field representing email in LoginRequest. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** @brief Field representing password in LoginRequest. */
    @NotBlank(message = "Password is required")
    private String password;
}
