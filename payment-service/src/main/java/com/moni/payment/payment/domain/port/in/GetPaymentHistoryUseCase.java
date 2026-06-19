package com.moni.payment.payment.domain.port.in;

import com.moni.payment.payment.domain.model.Payment;

import java.util.List;
import java.util.UUID;

public interface GetPaymentHistoryUseCase {

    List<Payment> getHistory(UUID userId);
}
