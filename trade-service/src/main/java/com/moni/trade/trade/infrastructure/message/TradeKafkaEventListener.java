package com.moni.trade.trade.infrastructure.message;

import com.moni.trade.trade.infrastructure.message.event.TradeCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeKafkaEventListener {

    private final TradeEventPublisher tradeEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTradeCompleted(TradeCompletedEvent event) {
        try {
            tradeEventPublisher.publishTradeCompleted(event);
        } catch (Exception e) {
            log.error("Kafka 발행 실패 - 거래는 정상 처리됨 tradeId={}", event.tradeId(), e);
        }
    }
}
