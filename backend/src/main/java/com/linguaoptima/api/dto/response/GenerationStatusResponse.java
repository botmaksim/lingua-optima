/**
 * @file GenerationStatusResponse.java
 * @brief Response DTO representing active AI generation status and device binding state.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Encapsulates whether an AI generation is currently running for the user and which device holds the lock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationStatusResponse {

    /** @brief True if an AI task generation is currently in flight. */
    private boolean isGenerating;

    /** @brief Device ID currently holding the active generation lease, if any. */
    private String activeDeviceId;

    /** @brief True if the active generation lease belongs to the caller's device. */
    private boolean isCurrentDevice;
}
