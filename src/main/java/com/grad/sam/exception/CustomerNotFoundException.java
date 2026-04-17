package com.grad.sam.exception;

/**
 * Thrown when the customer linked to an alert's account cannot be resolved.
 *
 * <p>Every alert must belong to an account that has an associated customer.
 * If this exception is raised the account/customer relationship should be
 * inspected in the database.</p>
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Integer alertId) {
        super("No customer found for the account linked to alert: " + alertId
              + ". Cannot open investigation without a valid customer.");
    }
}
