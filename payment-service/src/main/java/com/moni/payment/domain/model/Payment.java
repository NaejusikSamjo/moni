package com.moni.payment.domain.model;

import com.moni.payment.domain.event.PaymentCompletedEvent;
import com.moni.payment.domain.event.PaymentFailedEvent;
import com.moni.payment.domain.event.PaymentInitiatedEvent;
import com.moni.payment.infrastructure.persistence.converter.MerchantIdConverter;
import com.moni.payment.infrastructure.persistence.converter.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    private UUID id;

    @Convert(converter = MerchantIdConverter.class)
    @Column(name = "merchant_id", unique = true, nullable = false, length = 64)
    private MerchantId merchantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private Money amount;

    @Column(name = "pg_payment_key", unique = true)
    private String pgPaymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Transient
    private List<PaymentHistory> histories = new ArrayList<>();

    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    protected Payment() {}

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
