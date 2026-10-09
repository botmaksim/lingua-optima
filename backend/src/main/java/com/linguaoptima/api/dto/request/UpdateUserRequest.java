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

    /** @brief Field representing full name in UpdateUserRequest. */
    private String fullName;
    /** @brief Field representing display alias in UpdateUserRequest. */
    private String displayAlias;
    /** @brief Field representing cefr level in UpdateUserRequest. */
    private CefrLevel cefrLevel;
    /** @brief Field representing user role (e.g. STUDENT or TEACHER) in UpdateUserRequest. */
    private Role role;
}
