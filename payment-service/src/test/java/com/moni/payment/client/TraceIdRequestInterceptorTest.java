package com.moni.payment.client;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OTel Agent 없는 환경에서 Feign 요청에 trace_id/span_id 헤더가 올바르게 전파되는지 검증.
 *
 * OTel Agent 적용 환경에서는 W3C traceparent 헤더를 자동으로 주입하므로,
 * 이 인터셉터는 Agent 미적용 환경(로컬 개발, 단위 테스트 등)을 위한 fallback 역할을 한다.
 */
@DisplayName("TraceIdRequestInterceptor — Feign trace 헤더 전파 단위 테스트")
class TraceIdRequestInterceptorTest {

    private TraceIdRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdRequestInterceptor();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("MDC에 trace_id가 있으면 X-Trace-Id 헤더가 추가된다")
    void shouldAddTraceIdHeaderWhenMdcHasTraceId() {
        MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).containsKey(TraceIdRequestInterceptor.HEADER_TRACE_ID);
        assertThat(template.headers().get(TraceIdRequestInterceptor.HEADER_TRACE_ID))
                .contains("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    @DisplayName("MDC에 span_id가 있으면 X-Span-Id 헤더가 추가된다")
    void shouldAddSpanIdHeaderWhenMdcHasSpanId() {
        MDC.put("span_id", "a3ce929d0e0e4736");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).containsKey(TraceIdRequestInterceptor.HEADER_SPAN_ID);
        assertThat(template.headers().get(TraceIdRequestInterceptor.HEADER_SPAN_ID))
                .contains("a3ce929d0e0e4736");
    }

    @Test
    @DisplayName("MDC에 trace_id와 span_id 모두 있으면 두 헤더가 모두 추가된다")
    void shouldAddBothHeadersWhenBothPresentInMdc() {
        MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("span_id", "a3ce929d0e0e4736");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers())
                .containsKey(TraceIdRequestInterceptor.HEADER_TRACE_ID)
                .containsKey(TraceIdRequestInterceptor.HEADER_SPAN_ID);
    }

    @Test
    @DisplayName("MDC가 비어 있으면 trace 헤더가 추가되지 않는다")
    void shouldNotAddHeadersWhenMdcIsEmpty() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers())
                .doesNotContainKey(TraceIdRequestInterceptor.HEADER_TRACE_ID)
                .doesNotContainKey(TraceIdRequestInterceptor.HEADER_SPAN_ID);
    }

    @Test
    @DisplayName("OTel Agent가 주입한 trace_id가 그대로 다운스트림 헤더로 전파된다")
    void shouldPropagateOtelInjectedTraceId() {
        // OTel Agent가 MDC에 trace_id를 주입한 상황을 시뮬레이션
        String otelTraceId = "0af7651916cd43dd8448eb211c80319c";
        MDC.put("trace_id", otelTraceId);
        MDC.put("span_id", "b7ad6b7169203331");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(TraceIdRequestInterceptor.HEADER_TRACE_ID))
                .as("OTel trace_id가 X-Trace-Id 헤더로 정확히 전달되어야 한다")
                .contains(otelTraceId);
        assertThat(template.headers().get(TraceIdRequestInterceptor.HEADER_SPAN_ID))
                .contains("b7ad6b7169203331");
    }
}
