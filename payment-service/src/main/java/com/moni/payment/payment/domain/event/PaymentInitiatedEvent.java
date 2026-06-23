package com.moni.payment.payment.domain.event;

import com.moni.payment.payment.domain.model.Money;
import com.moni.payment.payment.domain.model.PaymentType;

import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID paymentId,
        UUID userId,
        Money amount,
        PaymentType paymentType,
        Instant occurredAt) {

    public PaymentInitiatedEvent(UUID paymentId, UUID userId, Money amount, PaymentType paymentType) {
        this(paymentId, userId, amount, paymentType, Instant.now());
    }
}
