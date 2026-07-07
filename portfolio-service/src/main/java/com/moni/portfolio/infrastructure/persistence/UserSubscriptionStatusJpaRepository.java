package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserSubscriptionStatusJpaRepository extends JpaRepository<UserSubscriptionStatus, UUID> {

    Optional<UserSubscriptionStatus> findByUserId(UUID userId);
}
