package com.moni.payment.payment.domain.port.in;

import com.moni.payment.payment.domain.model.Money;
import com.moni.payment.payment.domain.model.Payment;
import com.moni.payment.payment.domain.model.PaymentType;

import java.util.UUID;

public interface InitiatePaymentUseCase {

    record InitiatePaymentCommand(
            UUID userId,
            Money amount,
            PaymentType paymentType,
            String requestedBy) {
    }

    Payment initiatePayment(InitiatePaymentCommand command);
}
