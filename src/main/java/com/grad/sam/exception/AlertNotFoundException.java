package com.grad.sam.exception;

/**
 * Thrown when an alert cannot be located by its ID.
 */
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Integer alertId) {
        super("Alert not found: " + alertId);
    }
}
