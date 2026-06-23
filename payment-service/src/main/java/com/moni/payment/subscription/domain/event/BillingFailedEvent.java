package com.moni.payment.subscription.domain.event;

import com.moni.payment.payment.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record BillingFailedEvent(
        UUID subscriptionId,
        UUID userId,
        Money amount,
        String reason,
        Instant occurredAt
) {
    public BillingFailedEvent(UUID subscriptionId, UUID userId, Money amount, String reason) {
        this(subscriptionId, userId, amount, reason, Instant.now());
    }
}
