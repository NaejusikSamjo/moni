package com.moni.payment.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEventMessage(
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Instant occurredAt,
        Object payload) {
}
