package com.moni.portfolio.domain.enums;

public enum SubscriptionStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    CANCELLING,
    CANCELLED,
    SUSPENDED;

    public boolean isPaidPlan() {
        return this == ACTIVE || this == CANCELLING;
    }
}
