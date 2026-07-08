package com.moni.portfolio.infrastructure.messaging;

public final class PaymentSubscriptionTopics {

    public static final String SUBSCRIPTION_SUCCEEDED = "payment.subscription.succeeded";
    public static final String SUBSCRIPTION_CANCELLED = "payment.subscription.cancelled";
    public static final String SUBSCRIPTION_SUSPENDED = "payment.subscription.suspended";

    private PaymentSubscriptionTopics() {
    }
}
