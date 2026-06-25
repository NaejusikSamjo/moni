package com.moni.payment.application.dto.comman;

import com.moni.payment.domain.model.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record CancelSubscriptionResult(
        UUID subscriptionId,
        SubscriptionStatus status,
        Instant cancelledAt) {
}
