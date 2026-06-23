package com.moni.payment.infrastructure.repository;

import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.persistence.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    Optional<SubscriptionJpaEntity> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.status = 'ACTIVE' AND s.nextBillingDate <= :date")
    List<SubscriptionJpaEntity> findActiveSubscriptionsDueBefore(@Param("date") LocalDate date);
}
