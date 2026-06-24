package com.moni.payment.application.service;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.GetSubscriptionStatusQuery;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService implements GetSubscriptionStatusQuery {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResult execute(UUID userId) {
        return subscriptionRepository.findCurrentByUserId(userId)
                .map(SubscriptionStatusResult::active)
                .orElse(SubscriptionStatusResult.inactive());
    }

    @Transactional(readOnly = true)
    public void checkNoActiveSubscription(UUID userId) {
        subscriptionRepository.findActiveByUserId(userId)
                .ifPresent(s -> {
                    throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
                });
    }
}
