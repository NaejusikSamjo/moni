package com.moni.portfolio.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.portfolio.domain.enums.SubscriptionStatus;
import com.moni.portfolio.domain.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_user_subscription_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_subscription_status_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_user_subscription_status_user", columnList = "user_id")
        }
)
public class UserSubscriptionStatus extends BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private boolean subscribed;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private SubscriptionStatus status;

    @Column(name = "last_event_type", length = 50, nullable = false)
    private String lastEventType;

    @Column(name = "last_occurred_at", nullable = false)
    private Instant lastOccurredAt;

    @Builder
    private UserSubscriptionStatus(
            UUID userId,
            UUID subscriptionId,
            boolean subscribed,
            SubscriptionStatus status,
            String lastEventType,
            Instant lastOccurredAt
    ) {
        this.userId = userId;
        this.subscriptionId = subscriptionId;
        this.subscribed = subscribed;
        this.status = status;
        this.lastEventType = lastEventType;
        this.lastOccurredAt = lastOccurredAt;
    }

    public static UserSubscriptionStatus create(
            UUID userId,
            UUID subscriptionId,
            boolean subscribed,
            SubscriptionStatus status,
            String lastEventType,
            Instant lastOccurredAt
    ) {
        return UserSubscriptionStatus.builder()
                .userId(userId)
                .subscriptionId(subscriptionId)
                .subscribed(subscribed)
                .status(status)
                .lastEventType(lastEventType)
                .lastOccurredAt(lastOccurredAt)
                .build();
    }

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UuidV7.generate();
        }
    }

    public boolean isPaidPlan() {
        return subscribed && status.isPaidPlan();
    }

    public boolean isSameOrAfterLastEvent(Instant occurredAt) {
        return lastOccurredAt.isBefore(occurredAt) || lastOccurredAt.equals(occurredAt);
    }

    public void update(
            UUID subscriptionId,
            boolean subscribed,
            SubscriptionStatus status,
            String lastEventType,
            Instant lastOccurredAt
    ) {
        this.subscriptionId = subscriptionId;
        this.subscribed = subscribed;
        this.status = status;
        this.lastEventType = lastEventType;
        this.lastOccurredAt = lastOccurredAt;
    }
}
