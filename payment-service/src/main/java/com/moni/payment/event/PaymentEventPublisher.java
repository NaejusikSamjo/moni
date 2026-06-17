package com.moni.payment.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    static final String TOPIC = "payment.completed";
    static final String TRACE_HEADER = "X-Trace-Id";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(String paymentId) {
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, paymentId, paymentId);

        // MDC에 trace_id가 있으면 Kafka 메시지 헤더에 전파
        // OTel Agent 적용 시 Agent가 W3C traceparent를 자동 주입하므로 이 헤더가 추가로 포함됨
        String traceId = MDC.get("trace_id");
        if (traceId != null) {
            record.headers().add(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record);
        log.info("payment.completed 이벤트 발행 paymentId={}", paymentId);
    }
}
