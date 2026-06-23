package com.moni.payment.payment.domain.port.out;

import com.moni.payment.payment.domain.model.Payment;

public interface SavePaymentPort {

    Payment save(Payment payment);
}
