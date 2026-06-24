package com.moni.payment.domain.event;

import com.moni.payment.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID userId,
        Money amount,
        String pgPaymentKey,
        String billingKeyValue,
        Instant occurredAt) {

    public PaymentCompletedEvent(UUID paymentId, UUID userId, Money amount, String pgPaymentKey, String billingKeyValue) {
        this(paymentId, userId, amount, pgPaymentKey, billingKeyValue, Instant.now());
    }
}
