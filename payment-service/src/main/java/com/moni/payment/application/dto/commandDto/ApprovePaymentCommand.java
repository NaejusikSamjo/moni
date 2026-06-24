package com.moni.payment.application.dto.commandDto;

import java.util.UUID;

public record ApprovePaymentCommand(
        UUID paymentId,
        String pgPaymentKey,
        String billingKeyValue,
        String rawResponse,
        String actor) {
}
