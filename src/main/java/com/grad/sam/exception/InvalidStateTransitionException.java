package com.grad.sam.exception;

import com.grad.sam.enums.InvestigationState;

/**
 * Thrown when an investigation state transition is not permitted by the
 * defined state machine (OPEN → UNDER_REVIEW → CLOSED).
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String ref,
                                           InvestigationState from,
                                           InvestigationState to) {
        super(String.format(
                "Invalid state transition for investigation %s: %s → %s",
                ref, from, to));
    }
}
