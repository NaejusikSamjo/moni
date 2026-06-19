package com.moni.payment.subscription.domain.port.out;

import com.moni.payment.subscription.domain.model.SubscriptionHistory;

public interface SaveSubscriptionHistoryPort {

    void save(SubscriptionHistory history);
}
