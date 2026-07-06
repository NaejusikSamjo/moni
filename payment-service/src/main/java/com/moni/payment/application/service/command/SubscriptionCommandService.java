package com.moni.payment.application.service.command;

import com.moni.payment.application.dto.command.ActivateSubscriptionCommand;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCommandService {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId());
        subscription.activate(BillingKey.of(command.billingKeyValue()));

        Subscription saved = subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reactivateSubscription(UUID subscriptionId, BillingKey billingKey) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.reactivateFromCancelling(billingKey);
        subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);
        log.info("구독 재활성화 완료: subscriptionId={}", subscriptionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeCancellation(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.completeCancellation("만료일 도래로 인한 구독 자동 해지");
        subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        log.info("구독 CANCELLED 전환 완료: subscriptionId={}", subscriptionId);
    }

    private Subscription loadSubscription(UUID subscriptionId) {
        return subscriptionJpaRepository.findById(subscriptionId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
