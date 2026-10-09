/**
 * @file OcrException.java
 * @brief Exception thrown during optical character recognition failures or image parsing errors.
 */
package com.linguaoptima.api.exception;

/**
 * @brief Exception thrown during optical character recognition failures or image parsing errors.
 */
public class OcrException extends RuntimeException {

    /**
     * @brief Constructs an OcrException with a detail message.
     * @param message Detailed reason for OCR failure.
     */
    public OcrException(String message) {
        super(message);
    }

    /**
     * @brief Constructs an OcrException with a detail message and underlying cause.
     * @param message Detailed reason for OCR failure.
     * @param cause Underlying Throwable causing the exception.
     */
    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
