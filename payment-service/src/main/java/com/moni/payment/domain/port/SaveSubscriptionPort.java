package com.moni.payment.domain.port;

import com.moni.payment.domain.model.Subscription;

public interface SaveSubscriptionPort {

    Subscription save(Subscription subscription);
}
