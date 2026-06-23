package com.moni.payment.application.usecase;

import com.moni.payment.domain.model.Subscription;

import java.util.UUID;

public interface GetSubscriptionStatusUseCase {

    Subscription getSubscriptionStatus(UUID userId);
}
