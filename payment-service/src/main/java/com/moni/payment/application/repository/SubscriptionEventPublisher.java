package com.moni.payment.application.repository;

import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;

public interface SubscriptionEventPublisher {

    void publishActivated(SubscriptionActivatedEvent event);

    void publishCancelled(SubscriptionCancelledEvent event);

    void publishBillingFailed(BillingFailedEvent event);
}
