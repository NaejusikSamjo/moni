package com.moni.payment.application.usecase;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.domain.model.Subscription;

public interface ActivateSubscriptionUseCase {

    Subscription activateSubscription(ActivateSubscriptionCommand command);
}
