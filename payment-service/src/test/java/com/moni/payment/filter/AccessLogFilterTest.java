package com.moni.payment.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.moni.logging.filter.AccessLogFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AccessLogFilter 단위 테스트")
class AccessLogFilterTest {

    private AccessLogFilter filter;
    private Logger accessLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        filter = new AccessLogFilter();
        MDC.clear();

        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        accessLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(listAppender);
        MDC.clear();
    }

    @Test
    @DisplayName("access log 이벤트에 elapsedMs MDC 필드가 포함된다")
    void shouldIncludeElapsedMsInAccessLog() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payment/history");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap())
                .containsKey("elapsedMs");
        assertThat(Long.parseLong(event.getMDCPropertyMap().get("elapsedMs")))
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("access log 메시지에 HTTP method, URI, status code가 포함된다")
    void shouldLogHttpMethodUriAndStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment/charge");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(listAppender.list).hasSize(1);
        String message = listAppender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains("POST")
                .contains("/api/payment/charge")
                .contains("201");
    }

    @Test
    @DisplayName("요청 완료 후 MDC에서 elapsedMs가 제거된다")
    void shouldRemoveElapsedMsFromMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("elapsedMs")).isNull();
    }

    @Test
    @DisplayName("체인 예외 발생 시에도 access log가 기록되고 MDC가 정리된다")
    void shouldLogAndCleanMdcOnException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/payment/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain errorChain = (req, res) -> {
            throw new IOException("downstream error");
        };

        assertThatCode(() -> filter.doFilter(request, response, errorChain))
                .isInstanceOf(IOException.class);

        assertThat(listAppender.list).hasSize(1);
        assertThat(MDC.get("elapsedMs")).isNull();
    }
}
