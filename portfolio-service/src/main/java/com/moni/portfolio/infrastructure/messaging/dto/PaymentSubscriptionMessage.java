package com.moni.portfolio.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentSubscriptionMessage(
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
