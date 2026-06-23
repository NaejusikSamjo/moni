package com.moni.payment.application.usecase;

import java.util.UUID;

public interface CancelSubscriptionUseCase {

    record CancelSubscriptionCommand(
            UUID subscriptionId,
            UUID userId,
            String reason) {
    }

    void cancelSubscription(CancelSubscriptionCommand command);
}
