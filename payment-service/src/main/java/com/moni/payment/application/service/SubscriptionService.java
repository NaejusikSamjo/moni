package com.moni.payment.application.service;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.application.repository.SubscriptionEventPublisher;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.ActivateSubscriptionUseCase;
import com.moni.payment.application.usecase.CancelSubscriptionUseCase;
import com.moni.payment.application.usecase.GetSubscriptionStatusQuery;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService implements GetSubscriptionStatusQuery, ActivateSubscriptionUseCase, CancelSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventPublisher subscriptionEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionStatus(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    @Override
    @Transactional
    public Subscription activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId());
        subscription.activate(BillingKey.of(command.billingKeyValue()));

        Subscription saved = subscriptionRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionRepository::saveHistory);

        subscription.pullDomainEvents().forEach(event -> {
            if (event instanceof SubscriptionActivatedEvent activatedEvent) {
                subscriptionEventPublisher.publishActivated(activatedEvent);
            }
        });

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
        return saved;
    }

    @Override
    @Transactional
    public void cancelSubscription(CancelSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .filter(s -> s.getUserId().equals(command.userId()))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscription.cancel(command.reason());
        subscriptionRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionRepository::saveHistory);

        subscription.pullDomainEvents().forEach(event -> {
            if (event instanceof SubscriptionCancelledEvent cancelledEvent) {
                subscriptionEventPublisher.publishCancelled(cancelledEvent);
            }
        });

        log.info("구독 CANCELLING 저장: subscriptionId={}, userId={}", command.subscriptionId(), command.userId());
    }
}
