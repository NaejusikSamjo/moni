package com.moni.payment.infrastructure.messaging;

import com.moni.payment.common.config.KafkaConfig;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
<<<<<<<< HEAD:payment-service/src/main/java/com/moni/payment/infrastructure/messaging/SubscriptionKafkaEventListener.java
========
import com.moni.payment.application.repository.SubscriptionEventPublisher;
>>>>>>>> 943c276fae8ca320ebb258244ff13af5aa46f624:payment-service/src/main/java/com/moni/payment/infrastructure/messaging/SubscriptionKafkaProducer.java
import com.moni.payment.infrastructure.messaging.dto.SubscriptionEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
<<<<<<<< HEAD:payment-service/src/main/java/com/moni/payment/infrastructure/messaging/SubscriptionKafkaEventListener.java
public class SubscriptionKafkaEventListener {
========
public class SubscriptionKafkaProducer implements SubscriptionEventPublisher {
>>>>>>>> 943c276fae8ca320ebb258244ff13af5aa46f624:payment-service/src/main/java/com/moni/payment/infrastructure/messaging/SubscriptionKafkaProducer.java

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        SubscriptionEventMessage message = new SubscriptionEventMessage(
                "SUBSCRIPTION_ACTIVATED",
                event.subscriptionId(),
                event.userId(),
                event.occurredAt(),
                event);
        send(KafkaConfig.TOPIC_SUBSCRIPTION_SUCCEEDED, event.subscriptionId().toString(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionCancelled(SubscriptionCancelledEvent event) {
        SubscriptionEventMessage message = new SubscriptionEventMessage(
                "SUBSCRIPTION_CANCELLED",
                event.subscriptionId(),
                event.userId(),
                event.occurredAt(),
                event);
        send(KafkaConfig.TOPIC_SUBSCRIPTION_CANCELLED, event.subscriptionId().toString(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillingFailed(BillingFailedEvent event) {
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
