package com.moni.payment.application.command;

import java.util.UUID;

public record FailPaymentCommand(
        UUID paymentId,
        String pgResponse,
        String actor) {
}
