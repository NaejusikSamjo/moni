package com.moni.payment.application.service;

import com.moni.payment.application.dto.commandDto.ActivateSubscriptionCommand;
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
    public void activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId());
        subscription.activate(BillingKey.of(command.billingKeyValue()));

        Subscription saved = subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
    }
}
