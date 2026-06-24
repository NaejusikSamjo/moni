package com.moni.payment.application.service;

import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.GetSubscriptionStatusQuery;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.Subscription;
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
    public Subscription getSubscriptionStatus(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public void checkNoActiveSubscription(UUID userId) {
        subscriptionRepository.findActiveByUserId(userId)
                .ifPresent(s -> {
                    throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
                });
    }
}
