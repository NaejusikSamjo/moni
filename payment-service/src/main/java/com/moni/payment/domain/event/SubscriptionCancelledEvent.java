package com.moni.payment.domain.event;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCancelledEvent(
        UUID subscriptionId,
        UUID userId,
        String reason,
        Instant occurredAt) {

    public SubscriptionCancelledEvent(UUID subscriptionId, UUID userId, String reason) {
        this(subscriptionId, userId, reason, Instant.now());
    }
}
