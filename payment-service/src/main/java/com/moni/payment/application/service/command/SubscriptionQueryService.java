package com.moni.payment.application.service.command;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService {

    private final SubscriptionJpaRepository subscriptionJpaRepository;

    @Transactional(readOnly = true)
    public SubscriptionStatusResult execute(UUID userId) {
        return subscriptionJpaRepository.findFirstByUserIdAndStatusIn(
                        userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLING))
                .map(SubscriptionStatusResult::active)
                .orElse(SubscriptionStatusResult.inactive());
    }

    @Transactional(readOnly = true)
    public void checkNoActiveSubscription(UUID userId) {
        subscriptionJpaRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findCancellingSubscription(UUID userId) {
        return subscriptionJpaRepository.findByUserIdAndStatus(userId, SubscriptionStatus.CANCELLING);
    }

    @Transactional(readOnly = true)
    public Subscription findSuspendedSubscription(UUID userId) {
        return subscriptionJpaRepository.findByUserIdAndStatus(userId, SubscriptionStatus.SUSPENDED)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
