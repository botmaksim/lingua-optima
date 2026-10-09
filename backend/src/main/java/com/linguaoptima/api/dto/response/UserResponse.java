/**
 * @file UserResponse.java
 * @brief Response DTO representing a user profile.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Response DTO representing a user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /** @brief Field representing id in UserResponse. */
    private UUID id;
    /** @brief Field representing email in UserResponse. */
    private String email;
    /** @brief Field representing full name in UserResponse. */
    private String fullName;
    /** @brief Field representing role in UserResponse. */
    private Role role;
    /** @brief Field representing cefr level in UserResponse. */
    private CefrLevel cefrLevel;
    /** @brief Field representing display alias in UserResponse. */
    private String displayAlias;
    /** @brief Field representing streak count in UserResponse. */
    private int streakCount;
    /** @brief Field representing freeze tokens in UserResponse. */
    private int freezeTokens;
    /** @brief Field representing last active date in UserResponse. */
    private LocalDate lastActiveDate;
    /** @brief Field representing level up suggested at in UserResponse. */
    private LocalDateTime levelUpSuggestedAt;
    /** @brief Field representing created at in UserResponse. */
    private LocalDateTime createdAt;

    /**
     * @brief Maps a domain User entity to a UserResponse DTO.
     * @param user Domain entity instance.
     * @return Mapped UserResponse DTO or null if input is null.
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .cefrLevel(user.getCefrLevel())
            .displayAlias(user.getDisplayAlias())
            .streakCount(user.getStreakCount())
            .freezeTokens(user.getFreezeTokens())
            .lastActiveDate(user.getLastActiveDate())
            .levelUpSuggestedAt(user.getLevelUpSuggestedAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
