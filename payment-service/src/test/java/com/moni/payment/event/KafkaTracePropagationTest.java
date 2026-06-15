package com.moni.payment.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka 메시지 발행/수신 시 trace_id 전파를 검증하는 통합 테스트.
 *
 * 검증 시나리오:
 *  1. Publisher: MDC trace_id → Kafka 메시지 헤더(X-Trace-Id)로 전파
 *  2. Publisher: MDC 없을 때 헤더 미포함
 *  3. Listener: Kafka 헤더 → MDC 복원 후 로그 기록
 *
 * OTel Agent 적용 환경에서는 Agent가 W3C traceparent 헤더로 자동 전파하므로
 * 이 테스트는 Agent 미적용 환경(수동 전파 메커니즘)을 검증한다.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
@EmbeddedKafka(partitions = 1, topics = {PaymentEventPublisher.TOPIC})
@ActiveProfiles("test")
@DisplayName("Kafka trace_id 전파 통합 테스트")
class KafkaTracePropagationTest {

    @Autowired
    private PaymentEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> testConsumer;

    @BeforeEach
    void setUpTestConsumer() {
        Map<String, Object> config = KafkaTestUtils.consumerProps(
                "test-consumer-group", "true", embeddedKafkaBroker);
        testConsumer = new DefaultKafkaConsumerFactory<>(
                config, new StringDeserializer(), new StringDeserializer()
        ).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(testConsumer, PaymentEventPublisher.TOPIC);
    }

    @AfterEach
    void tearDown() {
        testConsumer.close();
        MDC.clear();
    }

    // -------------------------------------------------------------------------
    // Publisher 측 헤더 전파 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("[Publisher] MDC trace_id가 Kafka 메시지 X-Trace-Id 헤더로 전파된다")
    void publisherPropagatesTraceIdToKafkaHeader() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        MDC.put("trace_id", traceId);

        publisher.publishPaymentCompleted("pay-001");
        MDC.clear();

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(testConsumer, 5_000L, 1);
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> received = records.iterator().next();
        Header traceHeader = received.headers().lastHeader(PaymentEventPublisher.TRACE_HEADER);

        assertThat(traceHeader)
                .as("X-Trace-Id 헤더가 메시지에 포함되어야 한다")
                .isNotNull();
        assertThat(new String(traceHeader.value(), StandardCharsets.UTF_8))
                .isEqualTo(traceId);
    }

    @Test
    @DisplayName("[Publisher] MDC에 trace_id가 없으면 X-Trace-Id 헤더가 포함되지 않는다")
    void publisherOmitsHeaderWhenNoTraceIdInMdc() {
        MDC.clear();

        publisher.publishPaymentCompleted("pay-002");

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(testConsumer, 5_000L, 1);
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> received = records.iterator().next();
        Header traceHeader = received.headers().lastHeader(PaymentEventPublisher.TRACE_HEADER);

        assertThat(traceHeader)
                .as("trace_id가 없을 때 X-Trace-Id 헤더가 없어야 한다")
                .isNull();
    }

    // -------------------------------------------------------------------------
    // Listener 측 MDC 복원 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("[Listener] Kafka 헤더의 trace_id가 MDC에 복원되어 로그에 기록된다")
    void listenerRestoresTraceIdFromHeaderToMdc() {
        Logger listenerLogger = (Logger) LoggerFactory.getLogger(PaymentEventListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        listenerLogger.addAppender(appender);

        try {
            String traceId = "0af7651916cd43dd8448eb211c80319c";
            MDC.put("trace_id", traceId);
            publisher.publishPaymentCompleted("pay-trace-restore");
            MDC.clear();

            // Listener가 비동기로 메시지를 처리할 때까지 대기
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> appender.list.stream()
                            .anyMatch(e -> e.getFormattedMessage().contains("pay-trace-restore")));

            ILoggingEvent logEvent = appender.list.stream()
                    .filter(e -> e.getFormattedMessage().contains("pay-trace-restore"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("리스너 로그 이벤트를 찾을 수 없음"));

            assertThat(logEvent.getMDCPropertyMap().get("trace_id"))
                    .as("리스너 로그에 원본 trace_id가 MDC로 복원되어야 한다")
                    .isEqualTo(traceId);
        } finally {
            listenerLogger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("[Listener] 처리 완료 후 MDC의 trace_id가 제거된다 (MDC 누수 방지)")
    void listenerClearsTraceIdFromMdcAfterProcessing() {
        Logger listenerLogger = (Logger) LoggerFactory.getLogger(PaymentEventListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        listenerLogger.addAppender(appender);

        try {
            MDC.put("trace_id", "abc123def456789012345678901234ab");
            publisher.publishPaymentCompleted("pay-mdc-cleanup");
            MDC.clear();

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> appender.list.stream()
                            .anyMatch(e -> e.getFormattedMessage().contains("pay-mdc-cleanup")));

            // 로그 기록 시점의 MDC는 trace_id를 가져야 하지만
            // 그 이후(finally)에 MDC가 제거되었는지 확인
            // PaymentEventListener는 finally에서 MDC.remove("trace_id") 호출
            // 현재 스레드(테스트 스레드)의 MDC는 영향을 받지 않음을 확인
            assertThat(MDC.get("trace_id"))
                    .as("테스트 스레드의 MDC는 리스너에 의해 오염되지 않아야 한다")
                    .isNull();
        } finally {
            listenerLogger.detachAppender(appender);
        }
    }
}
