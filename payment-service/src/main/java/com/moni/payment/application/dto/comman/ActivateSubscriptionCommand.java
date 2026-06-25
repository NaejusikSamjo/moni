package com.moni.payment.application.dto.comman;

import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue) {
}
