package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private UUID id;
    private String name;
    private int studentCount;
    private Double avgScore;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<UserResponse> students = new ArrayList<>();
}
