package com.moni.payment.domain.event;

import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID userId,
        Money amount,
        String pgPaymentKey,
        String billingKeyValue,
        PaymentType paymentType,
        Instant occurredAt) {

    public PaymentCompletedEvent(UUID paymentId, UUID userId, Money amount, String pgPaymentKey,
            String billingKeyValue, PaymentType paymentType) {
        this(paymentId, userId, amount, pgPaymentKey, billingKeyValue, paymentType, Instant.now());
    }
}
