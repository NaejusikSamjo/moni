package com.moni.payment.subscription.domain.port.out;

import com.moni.payment.subscription.domain.model.Subscription;

public interface SaveSubscriptionPort {

    Subscription save(Subscription subscription);
}
