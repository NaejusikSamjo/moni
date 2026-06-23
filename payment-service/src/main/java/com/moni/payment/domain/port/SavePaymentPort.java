package com.moni.payment.domain.port;

import com.moni.payment.domain.model.Payment;

public interface SavePaymentPort {

    Payment save(Payment payment);
}
