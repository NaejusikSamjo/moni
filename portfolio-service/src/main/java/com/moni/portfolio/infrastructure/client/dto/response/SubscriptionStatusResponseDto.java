package com.moni.portfolio.infrastructure.client.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionStatusResponseDto(
        boolean subscribed,
        UUID subscriptionId,
        String status,
        LocalDate nextBillingDate,
        Long amount
) {
    public boolean isPaidPlan() {
        return subscribed && ("ACTIVE".equals(status) || "CANCELLING".equals(status));
    }
}
