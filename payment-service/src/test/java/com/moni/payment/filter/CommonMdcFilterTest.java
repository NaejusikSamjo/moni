package com.moni.payment.filter;

import com.moni.logging.filter.CommonMdcFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CommonMdcFilter 단위 테스트")
class CommonMdcFilterTest {

    private CommonMdcFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CommonMdcFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("trace_id, span_id 없을 때 fallback 값으로 자동 생성된다")
    void shouldGenerateTraceIdWhenAbsent() throws Exception {
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        AtomicReference<String> capturedSpanId = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            capturedTraceId.set(MDC.get("trace_id"));
            capturedSpanId.set(MDC.get("span_id"));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId.get())
                .isNotNull()
                .hasSize(32)
                .matches("[0-9a-f]{32}");
        assertThat(capturedSpanId.get())
                .isNotNull()
                .hasSize(16)
                .matches("[0-9a-f]{16}");
    }

    @Test
    @DisplayName("OTel Agent가 trace_id를 미리 설정한 경우 덮어쓰지 않는다")
    void shouldPreserveExistingTraceId() throws Exception {
        String otelTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        MDC.put("trace_id", otelTraceId);

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> capturedTraceId.set(MDC.get("trace_id"));

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId.get()).isEqualTo(otelTraceId);
    }

    @Test
    @DisplayName("요청 처리 완료 후 MDC가 비워진다")
    void shouldClearMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap).isNullOrEmpty();
    }

    @Test
    @DisplayName("체인 처리 중 예외가 발생해도 MDC가 반드시 클리어된다")
    void shouldClearMdcEvenOnException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain errorChain = (req, res) -> {
            throw new IOException("simulated downstream error");
        };

        assertThatCode(() -> filter.doFilter(request, response, errorChain))
                .isInstanceOf(IOException.class);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
