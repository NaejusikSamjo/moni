package com.moni.payment.application.service;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.application.command.CancelSubscriptionCommand;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCommandService {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Subscription activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId());
        subscription.activate(BillingKey.of(command.billingKeyValue()));

        Subscription saved = subscriptionJpaRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionHistoryRepository::save);
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
        return saved;
    }

    @Transactional
    public void cancelSubscription(CancelSubscriptionCommand command) {
        Subscription subscription = subscriptionJpaRepository.findById(command.subscriptionId())
                .filter(s -> s.getUserId().equals(command.userId()))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscription.cancel(command.reason());
        subscriptionJpaRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionHistoryRepository::save);
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 CANCELLING 저장: subscriptionId={}, userId={}", command.subscriptionId(), command.userId());
    }
}
