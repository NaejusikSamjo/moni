package com.moni.payment.domain.model;

import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Subscription {

    private final UUID id;
    private final UUID userId;
    private BillingKey billingKey;
    private SubscriptionStatus status;
    private LocalDate nextBillingDate;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private final List<SubscriptionHistory> histories;
    private final List<Object> domainEvents;

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

    public static Subscription create(UUID userId, LocalDate nextBillingDate) {
        Instant now = Instant.now();
        return new Subscription(
                UUID.randomUUID(), userId, null,
                SubscriptionStatus.PENDING_ACTIVATION, nextBillingDate,
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
        this.updatedAt = Instant.now();

        histories.add(SubscriptionHistory.of(id, previousStatus, SubscriptionStatus.CANCELLING, reason));
        domainEvents.add(new SubscriptionCancelledEvent(id, userId, reason));
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

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BillingKey getBillingKey() {
        return billingKey;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<SubscriptionHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }
}
