package com.moni.payment.application.usecase;

import com.moni.payment.domain.model.Subscription;

import java.time.LocalDate;
import java.util.UUID;

public interface ActivateSubscriptionUseCase {

    record ActivateSubscriptionCommand(
            UUID userId,
            String billingKeyValue,
            LocalDate nextBillingDate) {
    }

    Subscription activateSubscription(ActivateSubscriptionCommand command);
}
