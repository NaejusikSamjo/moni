package com.moni.payment.payment.adapter.out.persistence;

import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.model.PaymentStatus;
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
@Table(name = "payment_history")
public class PaymentHistoryJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentJpaEntity payment;

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

    protected PaymentHistoryJpaEntity() {}

    public PaymentHistory toDomain() {
        return PaymentHistory.reconstitute(
                id,
                payment.getId(),
                fromStatus,
                toStatus,
                pgResponse,
                requestedAt,
                requestedBy,
                respondedAt
        );
    }

    public static PaymentHistoryJpaEntity fromDomain(PaymentHistory history, PaymentJpaEntity paymentEntity) {
        PaymentHistoryJpaEntity entity = new PaymentHistoryJpaEntity();
        entity.id = history.getId();
        entity.payment = paymentEntity;
        entity.fromStatus = history.getFromStatus();
        entity.toStatus = history.getToStatus();
        entity.pgResponse = history.getPgResponse();
        entity.requestedAt = history.getRequestedAt();
        entity.requestedBy = history.getRequestedBy();
        entity.respondedAt = history.getRespondedAt();
        return entity;
    }
}
