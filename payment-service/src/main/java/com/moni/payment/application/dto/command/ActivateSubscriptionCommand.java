package com.moni.payment.application.dto.command;

import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue) {
}
