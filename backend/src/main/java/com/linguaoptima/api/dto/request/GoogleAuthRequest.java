/**
 * @file GoogleAuthRequest.java
 * @brief Request payload DTO for authenticating or registering via Google OAuth2 ID token.
 */
package com.linguaoptima.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.linguaoptima.api.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request payload DTO for authenticating or registering via Google OAuth2 ID token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    /**
     * @brief Google OAuth2 ID token or credential issued by Google Identity Services.
     */
    @NotBlank(message = "Google ID token is required")
    @JsonAlias({"credential", "token", "accessToken"})
    private String idToken;

    /**
     * @brief Optional role requested if a new account is created during Google sign-in.
     */
    private Role role;
}
