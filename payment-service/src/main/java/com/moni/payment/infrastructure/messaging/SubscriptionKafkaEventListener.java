package com.moni.payment.infrastructure.messaging;

import com.moni.payment.common.config.KafkaConfig;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
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
public class SubscriptionKafkaEventListener {

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
