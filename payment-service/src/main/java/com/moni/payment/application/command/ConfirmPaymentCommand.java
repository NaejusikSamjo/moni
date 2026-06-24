package com.moni.payment.application.command;

import java.time.Instant;

public record ConfirmPaymentCommand(
        String merchantId,
        String pgPaymentKey,
        String pgResponse,
        Instant respondedAt,
        String confirmedBy) {
}
