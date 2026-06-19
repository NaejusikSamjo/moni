package com.moni.payment.payment.domain.event;

import com.moni.payment.payment.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID userId,
        Money amount,
        String pgPaymentKey,
        Instant occurredAt) {

    public PaymentCompletedEvent(UUID paymentId, UUID userId, Money amount, String pgPaymentKey) {
        this(paymentId, userId, amount, pgPaymentKey, Instant.now());
    }
}
