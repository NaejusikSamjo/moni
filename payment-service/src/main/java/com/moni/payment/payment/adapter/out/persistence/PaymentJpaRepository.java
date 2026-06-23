package com.moni.payment.payment.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByMerchantId(String merchantId);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<PaymentJpaEntity> findByUserIdPaged(@Param("userId") UUID userId, Pageable pageable);
}
