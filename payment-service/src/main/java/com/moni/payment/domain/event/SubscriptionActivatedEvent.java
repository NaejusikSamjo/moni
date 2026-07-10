package com.moni.payment.domain.event;

import com.moni.payment.domain.model.BillingKey;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivatedEvent(
        UUID subscriptionId,
        UUID userId,
        BillingKey billingKey,
        Instant occurredAt) {

    public SubscriptionActivatedEvent(UUID subscriptionId, UUID userId, BillingKey billingKey) {
        this(subscriptionId, userId, billingKey, Instant.now());
    }
}
