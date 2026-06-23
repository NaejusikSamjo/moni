package com.moni.payment.subscription.adapter.in.web.dto;

import com.moni.payment.subscription.domain.model.Subscription;
import com.moni.payment.subscription.domain.model.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionStatusResponse(
        UUID subscriptionId,
        SubscriptionStatus status,
        LocalDate nextBillingDate,
        long amount) {

    private static final long SUBSCRIPTION_AMOUNT = 9900L;

    public static SubscriptionStatusResponse from(Subscription subscription) {
        return new SubscriptionStatusResponse(
                subscription.getId(),
                subscription.getStatus(),
                subscription.getNextBillingDate(),
                SUBSCRIPTION_AMOUNT);
    }
}
