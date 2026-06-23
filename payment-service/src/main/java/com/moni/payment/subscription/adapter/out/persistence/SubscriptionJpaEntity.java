package com.moni.payment.subscription.adapter.out.persistence;

import com.moni.payment.subscription.domain.model.BillingKey;
import com.moni.payment.subscription.domain.model.Subscription;
import com.moni.payment.subscription.domain.model.SubscriptionHistory;
import com.moni.payment.subscription.domain.model.SubscriptionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class SubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "billing_key")
    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "subscription", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private List<SubscriptionHistoryJpaEntity> histories = new ArrayList<>();

    protected SubscriptionJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public Subscription toDomain() {
        BillingKey domainBillingKey = billingKey != null ? BillingKey.of(billingKey) : null;
        List<SubscriptionHistory> historyList = histories.stream()
                .map(SubscriptionHistoryJpaEntity::toDomain)
                .toList();
        return Subscription.reconstitute(
                id, userId, domainBillingKey, status,
                nextBillingDate, createdAt, updatedAt, version, historyList
        );
    }

    public static SubscriptionJpaEntity fromDomain(Subscription subscription) {
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.id = subscription.getId();
        entity.userId = subscription.getUserId();
        entity.billingKey = subscription.getBillingKey() != null
                ? subscription.getBillingKey().getValue()
                : null;
        entity.status = subscription.getStatus();
        entity.nextBillingDate = subscription.getNextBillingDate();
        entity.createdAt = subscription.getCreatedAt();
        entity.updatedAt = subscription.getUpdatedAt();
        entity.version = subscription.getVersion();
        entity.histories = subscription.getHistories().stream()
                .map(h -> SubscriptionHistoryJpaEntity.fromDomain(h, entity))
                .collect(java.util.stream.Collectors.toList());
        return entity;
    }
}
