package com.moni.payment.application.dto.commandDto;

import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue) {
}
