package com.grad.sam.enums;

public enum TxnStatus {
    COMPLETED,
    PENDING,
    SCREENED,
    BLOCKED,
    REVERSED,
    FAILED;

    public boolean isScreenable() {
        return this == COMPLETED || this == PENDING;
    }
}
