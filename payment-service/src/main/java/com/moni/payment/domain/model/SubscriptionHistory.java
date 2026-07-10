package com.moni.payment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p_subscription_history")
public class SubscriptionHistory {

    @Id
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private SubscriptionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private SubscriptionStatus toStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected SubscriptionHistory() {}

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
