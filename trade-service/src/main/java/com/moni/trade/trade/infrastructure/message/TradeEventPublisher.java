package com.moni.trade.trade.infrastructure.message;

import com.moni.trade.trade.infrastructure.message.event.TradeCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventPublisher {

    private static final String TOPIC = "trade.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTradeCompleted(TradeCompletedEvent event) {
        kafkaTemplate.send(TOPIC, event.accountId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("거래 이벤트 발행 실패 tradeId={}", event.tradeId(), ex);
                    } else {
                        log.info("거래 이벤트 발행 성공 tradeId={}", event.tradeId());
                    }
                });
    }
}
