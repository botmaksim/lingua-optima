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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private CefrLevel cefrLevel;
    private String displayAlias;
    private int streakCount;
    private int freezeTokens;
    private LocalDate lastActiveDate;
    private LocalDateTime levelUpSuggestedAt;
    private LocalDateTime createdAt;

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
