/**
 * @file ChangePasswordRequest.java
 * @brief Request DTO for authenticated password update.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for authenticated password update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /** @brief Field representing old password in ChangePasswordRequest. */
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    /** @brief Field representing new password in ChangePasswordRequest. */
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;
}
