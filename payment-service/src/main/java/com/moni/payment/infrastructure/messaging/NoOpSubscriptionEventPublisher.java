package com.moni.payment.infrastructure.messaging;

import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.application.repository.SubscriptionEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 미연동 환경용 no-op 이벤트 발행기.
 * kafka.enabled=true 설정 시 SubscriptionKafkaProducer로 교체된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSubscriptionEventPublisher implements SubscriptionEventPublisher {

    @Override
    public void publishActivated(SubscriptionActivatedEvent event) {
        log.info("[NoOp] SubscriptionActivatedEvent 무시 (Kafka 미활성화): subscriptionId={}, userId={}",
                event.subscriptionId(), event.userId());
    }

    @Override
    public void publishCancelled(SubscriptionCancelledEvent event) {
        log.info("[NoOp] SubscriptionCancelledEvent 무시 (Kafka 미활성화): subscriptionId={}, userId={}",
                event.subscriptionId(), event.userId());
    }

    @Override
    public void publishBillingFailed(BillingFailedEvent event) {
        log.info("[NoOp] BillingFailedEvent 무시 (Kafka 미활성화): subscriptionId={}, userId={}",
                event.subscriptionId(), event.userId());
    }
}
