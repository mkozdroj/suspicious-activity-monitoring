package com.grad.sam.exception;

/**
 * Thrown when an investigation cannot be located by its ID or reference.
 */
public class InvestigationNotFoundException extends RuntimeException {

    public InvestigationNotFoundException(Integer investigationId) {
        super("Investigation not found: " + investigationId);
    }

    public InvestigationNotFoundException(String ref) {
        super("Investigation not found for ref: " + ref);
    }
}
