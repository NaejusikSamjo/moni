package com.moni.portfolio.application.service;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import com.moni.portfolio.domain.enums.PaymentSubscriptionEventType;
import com.moni.portfolio.domain.repository.UserSubscriptionStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubscriptionStatusCommandService {

    private final UserSubscriptionStatusRepository userSubscriptionStatusRepository;

    @Transactional
    public void applyPaymentSubscriptionEvent(
            String eventType,
            UUID subscriptionId,
            UUID userId,
            Instant occurredAt
    ) {
        if (eventType == null || subscriptionId == null || userId == null || occurredAt == null) {
            log.warn(
                    "Payment 구독 이벤트 필수 값 누락: eventType={}, subscriptionId={}, userId={}, occurredAt={}",
                    eventType,
                    subscriptionId,
                    userId,
                    occurredAt
            );
            return;
        }

        Optional<PaymentSubscriptionEventType> subscriptionEventType = PaymentSubscriptionEventType.from(eventType);

        if (subscriptionEventType.isEmpty()) {
            log.warn("알 수 없는 Payment 구독 이벤트 수신: eventType={}, userId={}", eventType, userId);
            return;
        }

        PaymentSubscriptionEventType resolvedEventType = subscriptionEventType.get();
        userSubscriptionStatusRepository.findByUserId(userId)
                .ifPresentOrElse(
                        subscriptionStatus -> updateSubscriptionStatus(
                                subscriptionStatus,
                                resolvedEventType,
                                subscriptionId,
                                occurredAt
                        ),
                        () -> createSubscriptionStatus(
                                resolvedEventType,
                                subscriptionId,
                                userId,
                                occurredAt
                        )
                );
    }

    private void createSubscriptionStatus(
            PaymentSubscriptionEventType eventType,
            UUID subscriptionId,
            UUID userId,
            Instant occurredAt
    ) {
        UserSubscriptionStatus subscriptionStatus = UserSubscriptionStatus.create(
                userId,
                subscriptionId,
                eventType.isSubscribed(),
                eventType.getStatus(),
                eventType.getEventType(),
                occurredAt
        );
        userSubscriptionStatusRepository.save(subscriptionStatus);
    }

    private void updateSubscriptionStatus(
            UserSubscriptionStatus subscriptionStatus,
            PaymentSubscriptionEventType eventType,
            UUID subscriptionId,
            Instant occurredAt
    ) {
        if (!subscriptionStatus.isSameOrAfterLastEvent(occurredAt)) {
            log.info(
                    "오래된 Payment 구독 이벤트 무시: eventType={}, userId={}, occurredAt={}, lastOccurredAt={}",
                    eventType.getEventType(),
                    subscriptionStatus.getUserId(),
                    occurredAt,
                    subscriptionStatus.getLastOccurredAt()
            );
            return;
        }

        subscriptionStatus.update(
                subscriptionId,
                eventType.isSubscribed(),
                eventType.getStatus(),
                eventType.getEventType(),
                occurredAt
        );
    }
}
