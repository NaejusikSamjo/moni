package com.moni.payment.payment.domain.model;

import com.moni.payment.payment.domain.event.PaymentCompletedEvent;
import com.moni.payment.payment.domain.event.PaymentFailedEvent;
import com.moni.payment.payment.domain.event.PaymentInitiatedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final MerchantId merchantId;
    private final UUID userId;
    private final PaymentType paymentType;
    private final Money amount;
    private String pgPaymentKey;
    private PaymentStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private final List<PaymentHistory> histories;
    private final List<Object> domainEvents;

    private Payment(
            UUID id,
            MerchantId merchantId,
            UUID userId,
            PaymentType paymentType,
            Money amount,
            String pgPaymentKey,
            PaymentStatus status,
            Instant expiresAt,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy,
            List<PaymentHistory> histories) {
        this.id = id;
        this.merchantId = merchantId;
        this.userId = userId;
        this.paymentType = paymentType;
        this.amount = amount;
        this.pgPaymentKey = pgPaymentKey;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.histories = new ArrayList<>(histories);
        this.domainEvents = new ArrayList<>();
    }

    public static Payment initiate(
            UUID userId,
            MerchantId merchantId,
            Money amount,
            PaymentType paymentType,
            Instant expiresAt,
            String createdBy) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Payment payment = new Payment(
                id, merchantId, userId, paymentType, amount,
                null, PaymentStatus.PENDING, expiresAt,
                now, createdBy, now, createdBy,
                Collections.emptyList());
        payment.domainEvents.add(new PaymentInitiatedEvent(id, userId, amount, paymentType));
        return payment;
    }

    public static Payment reconstitute(
            UUID id,
            MerchantId merchantId,
            UUID userId,
            PaymentType paymentType,
            Money amount,
            String pgPaymentKey,
            PaymentStatus status,
            Instant expiresAt,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy,
            List<PaymentHistory> histories) {
        return new Payment(
                id, merchantId, userId, paymentType, amount, pgPaymentKey, status, expiresAt,
                createdAt, createdBy, updatedAt, updatedBy, histories);
    }

    public void complete(String pgPaymentKey, String pgResponse, Instant respondedAt, String actor) {
        this.status.validateTransitionTo(PaymentStatus.COMPLETED);
        PaymentStatus previousStatus = this.status;

        PaymentHistory history = PaymentHistory.of(
                this.id, previousStatus, PaymentStatus.COMPLETED, pgResponse, Instant.now(), actor);
        history.recordResponse(respondedAt);
        this.histories.add(history);

        this.status = PaymentStatus.COMPLETED;
        this.pgPaymentKey = pgPaymentKey;
        this.updatedAt = Instant.now();
        this.updatedBy = actor;

        domainEvents.add(new PaymentCompletedEvent(id, userId, amount, pgPaymentKey));
    }

    public void fail(String pgResponse, Instant respondedAt, String actor) {
        this.status.validateTransitionTo(PaymentStatus.FAILED);
        PaymentStatus previousStatus = this.status;

        PaymentHistory history = PaymentHistory.of(
                this.id, previousStatus, PaymentStatus.FAILED, pgResponse, Instant.now(), actor);
        history.recordResponse(respondedAt);
        this.histories.add(history);

        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
        this.updatedBy = actor;

        domainEvents.add(new PaymentFailedEvent(id, userId, amount));
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID getId() {
        return id;
    }

    public MerchantId getMerchantId() {
        return merchantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public Money getAmount() {
        return amount;
    }

    public String getPgPaymentKey() {
        return pgPaymentKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public List<PaymentHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }
}
