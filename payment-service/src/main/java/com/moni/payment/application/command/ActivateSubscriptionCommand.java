package com.moni.payment.application.command;

import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue) {
}
