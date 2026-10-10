/**
 * @file ConcurrentGenerationException.java
 * @brief Exception thrown when an AI task generation is requested while another generation is in flight.
 */
package com.linguaoptima.api.exception;

import lombok.Getter;

/**
 * @brief Exception representing HTTP 409 Conflict when concurrent generation is rejected.
 */
@Getter
public class ConcurrentGenerationException extends RuntimeException {

    /** @brief Device ID currently holding the active generation lease. */
    private final String activeDeviceId;

    /** @brief Indicates whether the calling client is permitted to take over generation control. */
    private final boolean canTakeover;

    /**
     * @brief Constructs a ConcurrentGenerationException with a message.
     * @param message Detailed reason.
     */
    public ConcurrentGenerationException(String message) {
        super(message);
        this.activeDeviceId = null;
        this.canTakeover = true;
    }

    /**
     * @brief Constructs a ConcurrentGenerationException with a message and the active device ID.
     * @param message Detailed reason.
     * @param activeDeviceId Device ID currently generating.
     */
    public ConcurrentGenerationException(String message, String activeDeviceId) {
        super(message);
        this.activeDeviceId = activeDeviceId;
        this.canTakeover = true;
    }
}
