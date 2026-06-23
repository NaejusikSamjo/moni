package com.moni.payment.application.usecase;

import com.moni.payment.application.command.SubscribeCommand;
import com.moni.payment.application.command.SubscribeResult;

public interface InitiatePaymentUseCase {

    SubscribeResult initiatePayment(SubscribeCommand command);
}
