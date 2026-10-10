/**
 * @file GroupInvitationResponse.java
 * @brief Response DTO representing a pending student cohort group invitation.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Response DTO representing a pending student cohort group invitation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitationResponse {

    /** @brief Unique identifier of the group student membership invitation record. */
    private UUID id;

    /** @brief Unique identifier of the inviting group cohort. */
    private UUID groupId;

    /** @brief Display name of the group cohort. */
    private String groupName;

    /** @brief Full name of the teacher who dispatched the invitation. */
    private String teacherName;

    /** @brief Registered email of the teacher who dispatched the invitation. */
    private String teacherEmail;

    /** @brief Timestamp when the invitation was sent. */
    private LocalDateTime createdAt;
}
