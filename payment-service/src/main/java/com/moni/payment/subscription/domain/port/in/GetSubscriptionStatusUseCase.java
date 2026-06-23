package com.moni.payment.subscription.domain.port.in;

import com.moni.payment.domain.model.Subscription;

import java.util.UUID;

public interface GetSubscriptionStatusUseCase {

    Subscription getSubscriptionStatus(UUID userId);
}
