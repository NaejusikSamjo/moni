package com.moni.payment.domain.model;

import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.domain.model.converter.BillingKeyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Entity
@Getter
@Table(name = "subscription")
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = BillingKeyConverter.class)
    @Column(name = "billing_key")
    private BillingKey billingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "billing_key_deleted_at")
    private Instant billingKeyDeletedAt;

    @Version
    private Long version;

    @Transient
    private List<SubscriptionHistory> histories = new ArrayList<>();

    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    protected Subscription() {}

    private Subscription(
            UUID id,
            UUID userId,
            BillingKey billingKey,
            SubscriptionStatus status,
            LocalDate nextBillingDate,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            List<SubscriptionHistory> histories) {
        this.id = id;
        this.userId = userId;
        this.billingKey = billingKey;
        this.status = status;
        this.nextBillingDate = nextBillingDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.histories = new ArrayList<>(histories);
        this.domainEvents = new ArrayList<>();
    }

    public static Subscription create(UUID userId) {
        Instant now = Instant.now();
        return new Subscription(
                UUID.randomUUID(), userId, null,
                SubscriptionStatus.PENDING_ACTIVATION, LocalDate.now().plusMonths(1),
                now, now, null,
                Collections.emptyList());
    }

    public static Subscription reconstitute(
            UUID id,
            UUID userId,
            BillingKey billingKey,
            SubscriptionStatus status,
            LocalDate nextBillingDate,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            List<SubscriptionHistory> histories) {
        return new Subscription(
                id, userId, billingKey, status, nextBillingDate, createdAt, updatedAt, version, histories);
    }

    public void activate(BillingKey billingKey) {
        this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);
        SubscriptionStatus previousStatus = this.status;

        this.billingKey = billingKey;
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.ACTIVE, "최초 결제 성공"));
        domainEvents.add(new SubscriptionActivatedEvent(id, userId, billingKey));
    }

    public void cancel(String reason) {
        this.status.validateTransitionTo(SubscriptionStatus.CANCELLING);
        SubscriptionStatus previousStatus = this.status;

        this.status = SubscriptionStatus.CANCELLING;
        // nextBillingDate는 스케줄러가 CANCELLED 전환 기준으로 사용하므로 유지
        this.billingKeyDeletedAt = Instant.now();
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.CANCELLING, reason));
        domainEvents.add(new SubscriptionCancelledEvent(id, userId, reason));
    }

    public void reactivateFromCancelling(BillingKey reactivatedBillingKey) {
        this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);
        SubscriptionStatus previousStatus = this.status;

        this.billingKey = reactivatedBillingKey;
        this.status = SubscriptionStatus.ACTIVE;
        this.nextBillingDate = LocalDate.now().plusMonths(1);
        this.billingKeyDeletedAt = null;
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.ACTIVE, "CANCELLING 상태에서 재구독"));
        domainEvents.add(new SubscriptionActivatedEvent(id, userId, reactivatedBillingKey));
    }

    public void completeCancellation(String reason) {
        this.status.validateTransitionTo(SubscriptionStatus.CANCELLED);
        SubscriptionStatus previousStatus = this.status;

        this.status = SubscriptionStatus.CANCELLED;
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.CANCELLED, reason));
    }

    public void suspend(String reason) {
        this.status.validateTransitionTo(SubscriptionStatus.SUSPENDED);
        SubscriptionStatus previousStatus = this.status;

        this.status = SubscriptionStatus.SUSPENDED;
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.SUSPENDED, reason));
    }

    public void reactivate(String reason) {
        this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);
        SubscriptionStatus previousStatus = this.status;

        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.ACTIVE, reason));
    }

    public void extendBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
        this.updatedAt = Instant.now();
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public List<SubscriptionHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }
}
