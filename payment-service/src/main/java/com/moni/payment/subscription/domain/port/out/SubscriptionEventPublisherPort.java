package com.moni.payment.subscription.domain.port.out;

import com.moni.payment.subscription.domain.event.BillingFailedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionCancelledEvent;

public interface SubscriptionEventPublisherPort {

    void publishActivated(SubscriptionActivatedEvent event);

    void publishCancelled(SubscriptionCancelledEvent event);

    void publishBillingFailed(BillingFailedEvent event);
}
