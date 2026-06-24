package com.moni.payment.application.dto.commandDto;

import java.util.UUID;

public record FailPaymentCommand(
        UUID paymentId,
        String pgResponse,
        String actor) {
}
