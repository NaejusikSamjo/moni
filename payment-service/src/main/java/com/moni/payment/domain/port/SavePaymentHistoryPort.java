package com.moni.payment.domain.port;

import com.moni.payment.domain.model.PaymentHistory;

public interface SavePaymentHistoryPort {

    void save(PaymentHistory history);
}
