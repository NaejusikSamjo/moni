package com.moni.payment.application.usecase;

import com.moni.payment.application.dto.SubscriptionStatusResult;

import java.util.UUID;

public interface GetSubscriptionStatusQuery {

    SubscriptionStatusResult execute(UUID userId);
}
