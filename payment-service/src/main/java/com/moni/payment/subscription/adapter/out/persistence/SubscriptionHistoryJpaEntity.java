package com.moni.payment.subscription.adapter.out.persistence;

import com.moni.payment.subscription.domain.model.SubscriptionHistory;
import com.moni.payment.subscription.domain.model.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_history")
public class SubscriptionHistoryJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

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

    protected SubscriptionHistoryJpaEntity() {}

    public SubscriptionHistory toDomain() {
        return SubscriptionHistory.reconstitute(
                id,
                subscription.getId(),
                fromStatus,
                toStatus,
                reason,
                changedAt
        );
    }

    public static SubscriptionHistoryJpaEntity fromDomain(
            SubscriptionHistory history, SubscriptionJpaEntity subscriptionEntity) {
        SubscriptionHistoryJpaEntity entity = new SubscriptionHistoryJpaEntity();
        entity.id = history.getId();
        entity.subscription = subscriptionEntity;
        entity.fromStatus = history.getFromStatus();
        entity.toStatus = history.getToStatus();
        entity.reason = history.getReason();
        entity.changedAt = history.getChangedAt();
        return entity;
    }
}
