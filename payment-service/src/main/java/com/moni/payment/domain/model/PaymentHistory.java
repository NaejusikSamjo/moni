package com.moni.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PaymentHistory {

    private final UUID id;
    private final UUID paymentId;
    private final PaymentStatus fromStatus;
    private final PaymentStatus toStatus;
    private final String pgResponse;
    private final Instant requestedAt;
    private final String requestedBy;
    private Instant respondedAt;

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
