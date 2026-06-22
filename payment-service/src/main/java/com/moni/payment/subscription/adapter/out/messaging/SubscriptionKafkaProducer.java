package com.moni.payment.subscription.adapter.out.messaging;

import com.moni.payment.common.config.KafkaConfig;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.subscription.adapter.out.messaging.dto.SubscriptionEventMessage;
import com.moni.payment.subscription.domain.event.BillingFailedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.subscription.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.subscription.domain.port.out.SubscriptionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class SubscriptionKafkaProducer implements SubscriptionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishActivated(SubscriptionActivatedEvent event) {
        SubscriptionEventMessage message = new SubscriptionEventMessage(
                "SUBSCRIPTION_ACTIVATED",
                event.subscriptionId(),
                event.userId(),
                event.occurredAt(),
                event);
        send(KafkaConfig.TOPIC_SUBSCRIPTION_SUCCEEDED, event.subscriptionId().toString(), message);
    }

    @Override
    public void publishCancelled(SubscriptionCancelledEvent event) {
        SubscriptionEventMessage message = new SubscriptionEventMessage(
                "SUBSCRIPTION_CANCELLED",
                event.subscriptionId(),
                event.userId(),
                event.occurredAt(),
                event);
        send(KafkaConfig.TOPIC_SUBSCRIPTION_CANCELLED, event.subscriptionId().toString(), message);
    }

    @Override
    public void publishBillingFailed(BillingFailedEvent event) {
        SubscriptionEventMessage message = new SubscriptionEventMessage(
                "BILLING_FAILED",
                event.subscriptionId(),
                event.userId(),
                event.occurredAt(),
                event);
        send(KafkaConfig.TOPIC_BILLING_FAILED, event.subscriptionId().toString(), message);
    }

    private void send(String topic, String key, Object message) {
        try {
            kafkaTemplate.send(topic, key, message).get();
            log.info("Kafka 메시지 발행 완료: topic={}, key={}", topic, key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka 메시지 발행 중 인터럽트: topic={}, key={}", topic, key, e);
            throw new PaymentException(PaymentErrorCode.KAFKA_PUBLISH_FAILED);
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 실패: topic={}, key={}", topic, key, e);
            throw new PaymentException(PaymentErrorCode.KAFKA_PUBLISH_FAILED);
        }
    }
}
