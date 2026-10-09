/**
 * @file QuotaExceededException.java
 * @brief Exception thrown when rate limits or subscription tier usage quotas are exhausted.
 */
package com.linguaoptima.api.exception;

/**
 * @brief Exception thrown when rate limits or subscription tier usage quotas are exhausted.
 */
public class QuotaExceededException extends RuntimeException {

    /**
     * @brief Constructs a QuotaExceededException with an explanatory message.
     * @param message Detailed reason for quota exhaustion.
     */
    public QuotaExceededException(String message) {
        super(message);
    }
}
