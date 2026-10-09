/**
 * @file ForbiddenException.java
 * @brief Exception thrown when an authenticated user lacks permission to access a resource.
 */
package com.linguaoptima.api.exception;

/**
 * @brief Exception thrown when an authenticated user lacks permission to access a resource.
 */
public class ForbiddenException extends RuntimeException {

    /**
     * @brief Constructs a ForbiddenException with an explanatory message.
     * @param message Detailed reason describing the access violation.
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
