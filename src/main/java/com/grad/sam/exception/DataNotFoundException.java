package com.grad.sam.exception;

/**
 * Thrown when a requested entity cannot be located or a business rule
 * violation is detected during a data operation.
 *
 * <p>The message passed at the throw site describes exactly what went wrong,
 * making this the single exception used across all SAM services for
 * data-related failures.</p>
 */
public class DataNotFoundException extends RuntimeException {

    public DataNotFoundException(String message) {
        super(message);
    }
}

