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
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private PaymentStatus toStatus;

    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected PaymentHistory() {}

    private PaymentHistory(
            UUID id,
            UUID paymentId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String pgResponse,
            Instant requestedAt,
            String requestedBy) {
        this.id = id;
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.pgResponse = pgResponse;
        this.requestedAt = requestedAt;
        this.requestedBy = requestedBy;
    }

    public static PaymentHistory of(
            UUID paymentId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String pgResponse,
            Instant requestedAt,
            String requestedBy) {
        return new PaymentHistory(
                UUID.randomUUID(), paymentId, fromStatus, toStatus, pgResponse, requestedAt, requestedBy);
    }

    public static PaymentHistory reconstitute(
            UUID id,
            UUID paymentId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String pgResponse,
            Instant requestedAt,
            String requestedBy,
            Instant respondedAt) {
        PaymentHistory history = new PaymentHistory(
                id, paymentId, fromStatus, toStatus, pgResponse, requestedAt, requestedBy);
        history.respondedAt = respondedAt;
        return history;
    }

    public void recordResponse(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getFromStatus() {
        return fromStatus;
    }

    public PaymentStatus getToStatus() {
        return toStatus;
    }

    public String getPgResponse() {
        return pgResponse;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
