package com.moni.portfolio.application.service;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import com.moni.portfolio.domain.repository.UserSubscriptionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSubscriptionStatusQueryService {

    private final UserSubscriptionStatusRepository userSubscriptionStatusRepository;

    public boolean isPaidPlan(UUID userId) {
        return userSubscriptionStatusRepository.findByUserId(userId)
                .map(UserSubscriptionStatus::isPaidPlan)
                .orElse(false);
    }
}
