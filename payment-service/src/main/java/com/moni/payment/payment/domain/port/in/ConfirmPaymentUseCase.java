package com.moni.payment.payment.domain.port.in;

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
