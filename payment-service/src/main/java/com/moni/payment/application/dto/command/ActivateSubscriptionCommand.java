package com.moni.payment.application.dto.command;

import com.moni.payment.domain.model.Money;

import java.util.UUID;

public record ActivateSubscriptionCommand(
        UUID userId,
        String billingKeyValue,
        Money amount) {
}
