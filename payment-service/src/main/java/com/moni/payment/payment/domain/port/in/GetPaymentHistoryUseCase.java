package com.moni.payment.payment.domain.port.in;

import com.moni.payment.domain.model.Payment;

import java.util.List;
import java.util.UUID;

public interface GetPaymentHistoryUseCase {

    record GetPaymentHistoryQuery(UUID userId, int page, int size) {}

    List<Payment> getHistory(GetPaymentHistoryQuery query);
}
