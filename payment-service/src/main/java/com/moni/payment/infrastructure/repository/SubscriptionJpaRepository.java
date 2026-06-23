package com.moni.payment.infrastructure.repository;

import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.nextBillingDate <= :date")
    List<Subscription> findActiveSubscriptionsDueBefore(
            @Param("status") SubscriptionStatus status,
            @Param("date") LocalDate date);
}
