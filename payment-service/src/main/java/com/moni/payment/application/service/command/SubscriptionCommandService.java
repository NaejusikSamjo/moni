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

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCommandService {

    private static final int MAX_RETRY_COUNT = 3;

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activateSubscription(ActivateSubscriptionCommand command) {
        Subscription subscription = Subscription.create(command.userId());
        subscription.activate(BillingKey.of(command.billingKeyValue()), command.amount());

        Subscription saved = subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 ACTIVE 저장: subscriptionId={}, userId={}", saved.getId(), command.userId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extendBillingDate(UUID subscriptionId, LocalDate nextBillingDate) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.extendBillingDate(nextBillingDate);
        subscriptionJpaRepository.save(subscription);
        log.info("결제일 연장: subscriptionId={}, nextBillingDate={}", subscriptionId, nextBillingDate);
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
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);
        log.info("구독 CANCELLED 전환 완료: subscriptionId={}", subscriptionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailure(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.incrementRetryCount();
        if (subscription.getRetryCount() >= MAX_RETRY_COUNT) {
            subscription.suspend("정기결제 " + MAX_RETRY_COUNT + "회 연속 실패로 구독 정지");
            subscriptionHistoryRepository.saveAll(subscription.getHistories());
            log.warn("구독 SUSPENDED 전환: subscriptionId={}", subscriptionId);
        }
        subscriptionJpaRepository.save(subscription);
        log.info("결제 실패 처리: subscriptionId={}, retryCount={}", subscriptionId, subscription.getRetryCount());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetRetryCount(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.resetRetryCount();
        subscriptionJpaRepository.save(subscription);
        log.info("재시도 횟수 초기화: subscriptionId={}", subscriptionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reactivateFromSuspended(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.reactivate("사용자 재활성화 요청");
        subscription.resetRetryCount();
        subscription.extendBillingDate(LocalDate.now().plusMonths(1));
        subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);
        log.info("SUSPENDED 구독 재활성화 완료: subscriptionId={}", subscriptionId);
    }

    private Subscription loadSubscription(UUID subscriptionId) {
        return subscriptionJpaRepository.findById(subscriptionId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
