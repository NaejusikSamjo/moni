package com.moni.payment.application.dto.comman;

import java.util.UUID;

public record FailPaymentCommand(
        UUID paymentId,
        String pgResponse,
        String actor) {
}
