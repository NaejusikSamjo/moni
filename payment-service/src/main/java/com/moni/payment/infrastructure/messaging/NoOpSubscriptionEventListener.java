package com.moni.payment.infrastructure.messaging;

import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSubscriptionEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("[NoOp] SubscriptionActivatedEvent 무시 (Kafka 미활성화): subscriptionId={}, userId={}",
                event.subscriptionId(), event.userId());
    }
}
