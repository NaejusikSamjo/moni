package com.moni.payment.payment.domain.port.in;

import com.moni.payment.payment.application.command.SubscribeCommand;
import com.moni.payment.payment.application.command.SubscribeResult;

public interface InitiatePaymentUseCase {

    SubscribeResult initiatePayment(SubscribeCommand command);
}
