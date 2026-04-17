package com.grad.sam.exception;

/**
 * Thrown when a transaction cannot be located by its ID.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Integer txnId) {
        super("Transaction not found: " + txnId);
    }
}
