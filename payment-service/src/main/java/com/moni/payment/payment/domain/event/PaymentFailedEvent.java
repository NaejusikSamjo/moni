package com.moni.payment.payment.domain.event;

import com.moni.payment.payment.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID userId,
        Money amount,
        Instant occurredAt) {

    public PaymentFailedEvent(UUID paymentId, UUID userId, Money amount) {
        this(paymentId, userId, amount, Instant.now());
    }
}
