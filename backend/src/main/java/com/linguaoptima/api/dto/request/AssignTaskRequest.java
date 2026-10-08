package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTaskRequest {

    @NotEmpty(message = "At least one group ID must be provided")
    private List<UUID> groupIds;

    private LocalDateTime dueDate;
}
