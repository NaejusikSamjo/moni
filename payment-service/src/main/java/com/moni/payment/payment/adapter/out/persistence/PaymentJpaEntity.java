package com.moni.payment.payment.adapter.out.persistence;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "merchant_id", unique = true, nullable = false, length = 64)
    private String merchantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

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

    @OneToMany(mappedBy = "payment", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private List<PaymentHistoryJpaEntity> histories = new ArrayList<>();

    protected PaymentJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public Payment toDomain() {
        List<PaymentHistory> historyList = histories.stream()
                .map(PaymentHistoryJpaEntity::toDomain)
                .toList();
        return Payment.reconstitute(
                id, MerchantId.of(merchantId), userId,
                paymentType, Money.of(amount),
                pgPaymentKey, status,
                expiresAt, createdAt, createdBy, updatedAt, updatedBy,
                historyList
        );
    }

    public static PaymentJpaEntity fromDomain(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.id = payment.getId();
        entity.merchantId = payment.getMerchantId().getValue();
        entity.userId = payment.getUserId();
        entity.paymentType = payment.getPaymentType();
        entity.amount = payment.getAmount().getValue();
        entity.pgPaymentKey = payment.getPgPaymentKey();
        entity.status = payment.getStatus();
        entity.expiresAt = payment.getExpiresAt();
        entity.createdAt = payment.getCreatedAt();
        entity.createdBy = payment.getCreatedBy();
        entity.updatedAt = payment.getUpdatedAt();
        entity.updatedBy = payment.getUpdatedBy();
        entity.histories = payment.getHistories().stream()
                .map(h -> PaymentHistoryJpaEntity.fromDomain(h, entity))
                .collect(java.util.stream.Collectors.toList());
        return entity;
    }
}
