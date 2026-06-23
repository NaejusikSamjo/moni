package com.moni.payment.domain.port;

import com.moni.payment.domain.model.SubscriptionHistory;

public interface SaveSubscriptionHistoryPort {

    void save(SubscriptionHistory history);
}
