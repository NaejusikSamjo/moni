package com.moni.payment.application.command;

import java.util.UUID;

public record ApprovePaymentCommand(
        UUID paymentId,
        String pgPaymentKey,
        String billingKeyValue,
        String rawResponse,
        String actor) {
}
