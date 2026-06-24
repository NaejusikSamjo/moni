package com.moni.payment.presentation.dto;

import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(
        UUID id,
        BigDecimal amount,
        PaymentType paymentType,
        PaymentStatus status,
        String pgPaymentKey,
        Instant createdAt,
        Instant expiresAt) {

    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getAmount().getValue(),
                payment.getPaymentType(),
                payment.getStatus(),
                payment.getPgPaymentKey(),
                payment.getCreatedAt(),
                payment.getExpiresAt());
    }
}
