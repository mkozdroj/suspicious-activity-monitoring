package com.grad.sam.exception;

/**
 * Thrown when an investigation is closed without providing a findings summary.
 *
 * <p>Regulatory guidance requires that every closed investigation documents the
 * compliance officer's reasoning so the case is auditable and defensible during
 * an examination.</p>
 */
public class MissingFindingsException extends RuntimeException {

    public MissingFindingsException(String investigationRef) {
        super("Findings summary is required when closing investigation "
                + investigationRef
                + ". Please provide the compliance officer's rationale before closing.");
    }
}
