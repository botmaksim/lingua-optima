/**
 * @file ErrorResponse.java
 * @brief Standardized API error response payload with HTTP status, code, and validation messages.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @brief Standardized API error response payload with HTTP status, code, and validation messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /** @brief Field representing status in ErrorResponse. */
    private int status;
    /** @brief Field representing message in ErrorResponse. */
    private String message;
    /** @brief Field representing error code in ErrorResponse. */
    private String errorCode;
    /** @brief Field representing timestamp in ErrorResponse. */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    /** @brief Field representing errors in ErrorResponse. */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
