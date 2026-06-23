package com.moni.payment.subscription.adapter.out.messaging;

import com.moni.payment.subscription.domain.event.BillingFailedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.subscription.domain.port.out.SubscriptionEventPublisherPort;
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
public class NoOpSubscriptionEventPublisher implements SubscriptionEventPublisherPort {

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
