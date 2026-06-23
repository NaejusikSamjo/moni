package com.moni.payment.application.service;

import com.moni.payment.application.usecase.ActivateSubscriptionUseCase;
import com.moni.payment.application.usecase.GetSubscriptionStatusUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import com.moni.payment.domain.port.SaveSubscriptionHistoryPort;
import com.moni.payment.domain.port.SaveSubscriptionPort;
import com.moni.payment.domain.port.SubscriptionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService implements GetSubscriptionStatusUseCase, ActivateSubscriptionUseCase {

    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SaveSubscriptionPort saveSubscriptionPort;
    private final SaveSubscriptionHistoryPort saveSubscriptionHistoryPort;
    private final SubscriptionEventPublisherPort subscriptionEventPublisherPort;

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionStatus(UUID userId) {
        return loadSubscriptionPort.findActiveByUserId(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    @Override
    @Transactional
    public Subscription activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId(), command.nextBillingDate());
        subscription.activate(BillingKey.of(command.billingKeyValue()));

        Subscription saved = saveSubscriptionPort.save(subscription);
        subscription.getHistories().forEach(saveSubscriptionHistoryPort::save);

        subscription.pullDomainEvents().forEach(event -> {
            if (event instanceof SubscriptionActivatedEvent activatedEvent) {
                subscriptionEventPublisherPort.publishActivated(activatedEvent);
            }
        });

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
        return saved;
    }
}
