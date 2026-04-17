package com.grad.sam.exception;

/**
 * Thrown when an investigation already exists for the given alert.
 *
 * <p>Each alert may have at most one investigation, enforced by the UNIQUE
 * constraint on {@code investigation.alert_id}.</p>
 */
public class DuplicateInvestigationException extends RuntimeException {

    public DuplicateInvestigationException(Integer alertId) {
        super("Investigation already exists for alert: " + alertId);
    }
}
