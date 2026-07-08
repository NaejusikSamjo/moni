package com.moni.portfolio.domain.repository;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionStatusRepository {

    UserSubscriptionStatus save(UserSubscriptionStatus userSubscriptionStatus);

    Optional<UserSubscriptionStatus> findByUserId(UUID userId);
}
