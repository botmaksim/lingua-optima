/**
 * @file ResourceNotFoundException.java
 * @brief Exception thrown when an entity or resource identifier cannot be resolved.
 */
package com.linguaoptima.api.exception;

/**
 * @brief Exception thrown when an entity or resource identifier cannot be resolved.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @brief Constructs a ResourceNotFoundException with an explanatory message.
     * @param message Detailed reason indicating which entity or identifier is missing.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
