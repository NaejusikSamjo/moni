package com.moni.payment.presentation.dto;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.domain.model.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionStatusResponse(
        boolean subscribed,
        UUID subscriptionId,
        SubscriptionStatus status,
        LocalDate nextBillingDate,
        Long amount) {

    public static SubscriptionStatusResponse inactive() {
        return new SubscriptionStatusResponse(false, null, null, null, null);
    }

    public static SubscriptionStatusResponse from(SubscriptionStatusResult result) {
        return new SubscriptionStatusResponse(
                result.subscribed(),
                result.subscriptionId(),
                result.status(),
                result.nextBillingDate(),
                result.amount());
    }
}
