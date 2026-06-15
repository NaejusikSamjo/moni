package com.moni.payment.monitoring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.moni.logging.filter.AccessLogFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,metrics,prometheus",
        "management.endpoint.health.show-details=always",
        "management.endpoint.prometheus.enabled=true",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MDC 관찰가능성 통합 테스트")
class MdcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Logger accessLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        accessLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("HTTP 요청 시 access log에 trace_id MDC 필드가 존재한다")
    void httpRequestShouldProduceAccessLogWithTraceId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent accessLog = listAppender.list.get(0);
        Map<String, String> mdc = accessLog.getMDCPropertyMap();

        assertThat(mdc)
                .as("access log MDC에 trace_id 가 존재해야 한다")
                .containsKey("trace_id");
        assertThat(mdc.get("trace_id"))
                .as("trace_id 는 빈 값이면 안 된다")
                .isNotBlank();
    }

    @Test
    @DisplayName("HTTP 요청 시 access log에 elapsedMs MDC 필드가 존재한다")
    void httpRequestShouldProduceAccessLogWithElapsedMs() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent accessLog = listAppender.list.get(0);
        Map<String, String> mdc = accessLog.getMDCPropertyMap();

        assertThat(mdc).containsKey("elapsedMs");
        assertThat(Long.parseLong(mdc.get("elapsedMs")))
                .as("elapsedMs 는 0 이상이어야 한다")
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("연속 요청들은 각기 다른 trace_id를 가진다")
    void consecutiveRequestsShouldHaveDifferentTraceIds() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isOk());

        assertThat(listAppender.list).hasSizeGreaterThanOrEqualTo(2);

        String traceId1 = listAppender.list.get(0).getMDCPropertyMap().get("trace_id");
        String traceId2 = listAppender.list.get(1).getMDCPropertyMap().get("trace_id");

        assertThat(traceId1).isNotBlank();
        assertThat(traceId2).isNotBlank();
        assertThat(traceId1)
                .as("독립된 두 요청은 서로 다른 trace_id를 가져야 한다")
                .isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("access log 메시지에 요청 URI와 응답 상태코드가 포함된다")
    void accessLogShouldContainUriAndStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        assertThat(listAppender.list).isNotEmpty();
        String message = listAppender.list.get(0).getFormattedMessage();

        assertThat(message)
                .contains("GET")
                .contains("/actuator/health")
                .contains("200");
    }
}
