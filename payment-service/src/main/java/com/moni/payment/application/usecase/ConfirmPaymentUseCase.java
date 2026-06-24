package com.moni.payment.application.usecase;

import java.time.Instant;

public interface ConfirmPaymentUseCase {

    record ConfirmPaymentCommand(
            String merchantId,
            String pgPaymentKey,
            String pgResponse,
            Instant respondedAt,
            String confirmedBy) {
    }

    void confirmPayment(ConfirmPaymentCommand command);
}
