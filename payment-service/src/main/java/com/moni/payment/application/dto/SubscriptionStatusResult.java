package com.moni.payment.application.dto;

import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionStatusResult(
        boolean subscribed,
        UUID subscriptionId,
        SubscriptionStatus status,
        LocalDate nextBillingDate,
        Long amount) {

    private static final long SUBSCRIPTION_AMOUNT = 9900L;

    public static SubscriptionStatusResult inactive() {
        return new SubscriptionStatusResult(false, null, null, null, null);
    }

    public static SubscriptionStatusResult active(Subscription subscription) {
        return new SubscriptionStatusResult(
                true,
                subscription.getId(),
                subscription.getStatus(),
                subscription.getNextBillingDate(),
                SUBSCRIPTION_AMOUNT);
    }
}
