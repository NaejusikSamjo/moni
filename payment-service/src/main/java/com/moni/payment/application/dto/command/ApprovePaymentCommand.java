package com.moni.payment.application.dto.command;

import java.util.UUID;

public record ApprovePaymentCommand(
        UUID paymentId,
        String pgPaymentKey,
        String billingKeyValue,
        String rawResponse,
        String actor) {
}
