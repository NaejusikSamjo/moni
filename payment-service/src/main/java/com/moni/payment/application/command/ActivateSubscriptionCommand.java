package com.moni.payment.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue,
        LocalDate nextBillingDate) {
}
