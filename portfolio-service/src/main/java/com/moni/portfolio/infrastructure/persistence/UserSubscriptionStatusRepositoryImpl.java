package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import com.moni.portfolio.domain.repository.UserSubscriptionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserSubscriptionStatusRepositoryImpl implements UserSubscriptionStatusRepository {

    private final UserSubscriptionStatusJpaRepository userSubscriptionStatusJpaRepository;

    @Override
    public UserSubscriptionStatus save(UserSubscriptionStatus userSubscriptionStatus) {
        return userSubscriptionStatusJpaRepository.save(userSubscriptionStatus);
    }

    @Override
    public Optional<UserSubscriptionStatus> findByUserId(UUID userId) {
        return userSubscriptionStatusJpaRepository.findByUserId(userId);
    }
}
