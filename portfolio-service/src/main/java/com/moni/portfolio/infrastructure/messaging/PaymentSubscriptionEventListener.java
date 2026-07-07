package com.moni.portfolio.infrastructure.messaging;

import com.moni.portfolio.application.service.UserSubscriptionStatusCommandService;
import com.moni.portfolio.infrastructure.messaging.dto.PaymentSubscriptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSubscriptionEventListener {

    private final UserSubscriptionStatusCommandService userSubscriptionStatusCommandService;

    @KafkaListener(
            topics = {
                    PaymentSubscriptionTopics.SUBSCRIPTION_SUCCEEDED,
                    PaymentSubscriptionTopics.SUBSCRIPTION_CANCELLED,
                    PaymentSubscriptionTopics.SUBSCRIPTION_SUSPENDED
            },
            containerFactory = "paymentSubscriptionKafkaListenerContainerFactory"
    )
    public void handlePaymentSubscriptionEvent(PaymentSubscriptionMessage message) {
        if (message == null) {
            log.warn("Payment 구독 이벤트 메시지가 비어 있습니다.");
            return;
        }

        userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                message.eventType(),
                message.subscriptionId(),
                message.userId(),
                message.occurredAt()
        );
    }
}
