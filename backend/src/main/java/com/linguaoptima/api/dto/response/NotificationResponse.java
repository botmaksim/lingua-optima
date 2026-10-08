package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) return null;
        return NotificationResponse.builder()
            .id(notification.getId())
            .message(notification.getMessage())
            .type(notification.getType())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
