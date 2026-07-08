package com.moni.portfolio.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum PaymentSubscriptionEventType {

    SUBSCRIPTION_ACTIVATED("SUBSCRIPTION_ACTIVATED", SubscriptionStatus.ACTIVE, true),
    SUBSCRIPTION_CANCELLED("SUBSCRIPTION_CANCELLED", SubscriptionStatus.CANCELLED, false),
    SUBSCRIPTION_SUSPENDED("SUBSCRIPTION_SUSPENDED", SubscriptionStatus.SUSPENDED, false);

    private final String eventType;
    private final SubscriptionStatus status;
    private final boolean subscribed;

    public static Optional<PaymentSubscriptionEventType> from(String eventType) {
        return Arrays.stream(values())
                .filter(subscriptionEventType -> subscriptionEventType.eventType.equals(eventType))
                .findFirst();
    }
}
