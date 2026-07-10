package com.moni.payment.application.service;


import com.moni.payment.application.dto.command.CancelSubscriptionResult;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeCancelUseCase {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public CancelSubscriptionResult execute(UUID userId) {
        // 1. 현재 구독 중인지 확인한다
        Subscription subscription = subscriptionJpaRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. status, updatedAt 변경 / 3. nextBillingDate null 처리 / 4. SubscriptionHistory 기록
        subscription.cancel("사용자 요청에 의한 구독 해지");

        subscriptionJpaRepository.save(subscription);
        subscriptionHistoryRepository.saveAll(subscription.getHistories());
        subscription.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        log.info("구독 해지 완료: subscriptionId={}, userId={}", subscription.getId(), userId);

        return new CancelSubscriptionResult(
                subscription.getId(), subscription.getStatus(), subscription.getUpdatedAt());
    }
}
