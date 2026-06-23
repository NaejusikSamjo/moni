package com.moni.payment.application.usecase;

import com.moni.payment.domain.model.Payment;

import java.util.List;
import java.util.UUID;

public interface GetPaymentHistoryUseCase {

    record GetPaymentHistoryQuery(UUID userId, int page, int size) {}

    List<Payment> getHistory(GetPaymentHistoryQuery query);
}
