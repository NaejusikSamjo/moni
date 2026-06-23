package com.moni.payment.subscription.adapter.out.messaging;

import com.moni.payment.common.config.KafkaConfig;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.infrastructure.messaging.SubscriptionKafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionKafkaProducer")
class SubscriptionKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SubscriptionKafkaProducer producer;

    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final BillingKey BILLING_KEY = BillingKey.of("bk-test-001");

    @SuppressWarnings("unchecked")
    private void mockKafkaSendSuccess() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(null);
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);
    }

    @Nested
    @DisplayName("publishActivated()")
    class PublishActivated {

        @Test
        @DisplayName("SUBSCRIPTION_ACTIVATED 이벤트를 succeeded 토픽으로 발행한다")
        void publishesToSucceededTopic() {
            mockKafkaSendSuccess();
            SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(
                    SUBSCRIPTION_ID, USER_ID, BILLING_KEY);

            producer.publishActivated(event);

            verify(kafkaTemplate).send(
                    eq(KafkaConfig.TOPIC_SUBSCRIPTION_SUCCEEDED),
                    eq(SUBSCRIPTION_ID.toString()),
                    any());
        }

        @Test
        @DisplayName("Kafka 발행 실패 시 KAFKA_PUBLISH_FAILED 예외 발생")
        void failureThrowsException() {
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka unavailable"));
            given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

            SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(
                    SUBSCRIPTION_ID, USER_ID, BILLING_KEY);

            assertThatThrownBy(() -> producer.publishActivated(event))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.KAFKA_PUBLISH_FAILED);
        }
    }

    @Nested
    @DisplayName("publishCancelled()")
    class PublishCancelled {

        @Test
        @DisplayName("SUBSCRIPTION_CANCELLED 이벤트를 cancelled 토픽으로 발행한다")
        void publishesToCancelledTopic() {
            mockKafkaSendSuccess();
            SubscriptionCancelledEvent event = new SubscriptionCancelledEvent(
                    SUBSCRIPTION_ID, USER_ID, "사용자 요청");

            producer.publishCancelled(event);

            verify(kafkaTemplate).send(
                    eq(KafkaConfig.TOPIC_SUBSCRIPTION_CANCELLED),
                    eq(SUBSCRIPTION_ID.toString()),
                    any());
        }
    }

    @Nested
    @DisplayName("publishBillingFailed()")
    class PublishBillingFailed {

        @Test
        @DisplayName("BILLING_FAILED 이벤트를 billing.failed 토픽으로 발행한다")
        void publishesToBillingFailedTopic() {
            mockKafkaSendSuccess();
            BillingFailedEvent event = new BillingFailedEvent(
                    SUBSCRIPTION_ID, USER_ID, Money.of(9900), "카드 한도 초과");

            producer.publishBillingFailed(event);

            verify(kafkaTemplate).send(
                    eq(KafkaConfig.TOPIC_BILLING_FAILED),
                    eq(SUBSCRIPTION_ID.toString()),
                    any());
        }
    }
}
