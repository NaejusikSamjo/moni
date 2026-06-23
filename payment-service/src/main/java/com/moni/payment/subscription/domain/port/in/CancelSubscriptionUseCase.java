package com.moni.payment.subscription.domain.port.in;

import java.util.UUID;

public interface CancelSubscriptionUseCase {

    record CancelSubscriptionCommand(
            UUID subscriptionId,
            UUID userId,
            String reason) {
    }

    void cancelSubscription(CancelSubscriptionCommand command);
}
