package com.moni.payment.domain.port;

import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;

public interface SubscriptionEventPublisherPort {

    void publishActivated(SubscriptionActivatedEvent event);

    void publishCancelled(SubscriptionCancelledEvent event);

    void publishBillingFailed(BillingFailedEvent event);
}
