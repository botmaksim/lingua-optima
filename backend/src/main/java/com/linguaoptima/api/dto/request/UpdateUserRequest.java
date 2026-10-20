/**
 * @file UpdateUserRequest.java
 * @brief Request DTO for updating user profile attributes and CEFR level.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for updating user profile attributes and CEFR level.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;
    private String displayAlias;
    private CefrLevel cefrLevel;
    private Role role;
}
