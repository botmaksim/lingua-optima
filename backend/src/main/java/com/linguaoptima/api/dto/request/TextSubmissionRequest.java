package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSubmissionRequest {

    private UUID assignmentId;

    @NotBlank(message = "Text cannot be blank")
    private String text;

    /**
     * GRAMMAR, ESSAY, REWRITE, etc.
     */
    private String type;
}
