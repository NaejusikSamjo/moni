package com.moni.portfolio.infrastructure.client.dto.response;

import com.moni.portfolio.domain.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionStatusResponseDto(
        boolean subscribed,
        UUID subscriptionId,
        SubscriptionStatus status,
        LocalDate nextBillingDate,
        Long amount
) {
    public boolean isPaidPlan() {
        return subscribed && status != null && status.isPaidPlan();
    }
}
