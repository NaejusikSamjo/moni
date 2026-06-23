package com.moni.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public class SubscriptionHistory {

    private final UUID id;
    private final UUID subscriptionId;
    private final SubscriptionStatus fromStatus;
    private final SubscriptionStatus toStatus;
    private final String reason;
    private final Instant changedAt;

    private SubscriptionHistory(
            UUID id,
            UUID subscriptionId,
            SubscriptionStatus fromStatus,
            SubscriptionStatus toStatus,
            String reason,
            Instant changedAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public static SubscriptionHistory of(
            UUID subscriptionId,
            SubscriptionStatus fromStatus,
            SubscriptionStatus toStatus,
            String reason) {
        return new SubscriptionHistory(
                UUID.randomUUID(), subscriptionId, fromStatus, toStatus, reason, Instant.now());
    }

    public static SubscriptionHistory reconstitute(
            UUID id,
            UUID subscriptionId,
            SubscriptionStatus fromStatus,
            SubscriptionStatus toStatus,
            String reason,
            Instant changedAt) {
        return new SubscriptionHistory(id, subscriptionId, fromStatus, toStatus, reason, changedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public SubscriptionStatus getFromStatus() {
        return fromStatus;
    }

    public SubscriptionStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
