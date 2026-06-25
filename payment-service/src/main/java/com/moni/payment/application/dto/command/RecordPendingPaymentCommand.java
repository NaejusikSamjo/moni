package com.moni.payment.application.dto.command;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;

import java.time.Instant;
import java.util.UUID;

public record RecordPendingPaymentCommand(
        UUID userId,
        MerchantId merchantId,
        Money amount,
        PaymentType paymentType,
        Instant expiresAt,
        String actor) {
}
