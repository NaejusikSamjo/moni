package com.moni.payment.presentation.dto;

import com.moni.payment.application.dto.comman.CancelSubscriptionResult;
import com.moni.payment.domain.model.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record CancelSubscriptionResponse(
        UUID subscriptionId,
        SubscriptionStatus status,
        Instant cancelledAt) {

    public static CancelSubscriptionResponse from(CancelSubscriptionResult result) {
        return new CancelSubscriptionResponse(
                result.subscriptionId(),
                result.status(),
                result.cancelledAt());
    }
}
