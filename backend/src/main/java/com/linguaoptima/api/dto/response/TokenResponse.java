/**
 * @file TokenResponse.java
 * @brief Response DTO containing access token details and authenticated user profile.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO containing access token details and authenticated user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /** @brief Short-lived JWT access token string. */
    private String accessToken;

    /** @brief Internal refresh token passed to AuthController for HttpOnly cookie attachment. */
    @JsonIgnore
    private String refreshToken;

    /** @brief Token scheme identifier, defaulting to Bearer. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** @brief Access token validity lifetime in seconds (900s = 15m). */
    @Builder.Default
    private long expiresIn = 900;

    /** @brief Authenticated user profile metadata. */
    private UserResponse user;
}
