package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    @Builder.Default
    private long expiresIn = 900; // 15 minutes in seconds
    private UserResponse user;
}
