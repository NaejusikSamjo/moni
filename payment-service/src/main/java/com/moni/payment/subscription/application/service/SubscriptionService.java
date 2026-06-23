package com.moni.payment.subscription.application.service;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.subscription.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.subscription.domain.model.BillingKey;
import com.moni.payment.subscription.domain.model.Subscription;
import com.moni.payment.subscription.domain.port.in.ActivateSubscriptionUseCase;
import com.moni.payment.subscription.domain.port.in.GetSubscriptionStatusUseCase;
import com.moni.payment.subscription.domain.port.out.LoadSubscriptionPort;
import com.moni.payment.subscription.domain.port.out.SaveSubscriptionHistoryPort;
import com.moni.payment.subscription.domain.port.out.SaveSubscriptionPort;
import com.moni.payment.subscription.domain.port.out.SubscriptionEventPublisherPort;
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
