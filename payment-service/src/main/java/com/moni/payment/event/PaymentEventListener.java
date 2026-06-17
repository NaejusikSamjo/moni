package com.moni.payment.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    @KafkaListener(topics = PaymentEventPublisher.TOPIC, groupId = "payment-service")
    public void onPaymentCompleted(ConsumerRecord<String, String> record) {
        // OTel Agent 없는 환경: Kafka 메시지 헤더에서 trace_id를 읽어 MDC에 복원
        // OTel Agent 적용 시 Agent가 W3C traceparent 헤더를 자동으로 MDC에 주입
        String traceId = extractHeader(record, PaymentEventPublisher.TRACE_HEADER);
        try {
            if (traceId != null) {
                MDC.put("trace_id", traceId);
            }
            log.info("payment.completed 이벤트 수신 paymentId={}", record.value());
        } finally {
            MDC.remove("trace_id");
        }
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
}
