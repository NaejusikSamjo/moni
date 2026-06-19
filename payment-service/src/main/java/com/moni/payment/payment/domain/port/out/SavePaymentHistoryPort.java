package com.moni.payment.payment.domain.port.out;

import com.moni.payment.payment.domain.model.PaymentHistory;

public interface SavePaymentHistoryPort {

    void save(PaymentHistory history);
}
