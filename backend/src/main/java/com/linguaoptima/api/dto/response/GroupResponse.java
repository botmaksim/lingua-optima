/**
 * @file GroupResponse.java
 * @brief Response DTO representing a teacher study group and its enrolled students.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief Response DTO representing a teacher study group and its enrolled students.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    /** @brief Field representing id in GroupResponse. */
    private UUID id;
    /** @brief Field representing name in GroupResponse. */
    private String name;
    /** @brief Field representing student count in GroupResponse. */
    private int studentCount;
    /** @brief Field representing avg score in GroupResponse. */
    private Double avgScore;
    /** @brief Field representing created at in GroupResponse. */
    private LocalDateTime createdAt;
    /** @brief Field representing active enrolled students in GroupResponse. */
    @Builder.Default
    private List<UserResponse> students = new ArrayList<>();

    /** @brief Field representing students with pending group invitations in GroupResponse. */
    @Builder.Default
    private List<UserResponse> pendingStudents = new ArrayList<>();

    /** @brief Indicates if cohort is locked into read-only mode due to subscription tier limits. */
    @Builder.Default
    private boolean isLocked = false;
}
