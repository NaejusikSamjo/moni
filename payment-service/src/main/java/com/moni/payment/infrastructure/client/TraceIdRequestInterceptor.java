package com.moni.payment.infrastructure.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * OTel Agent 없는 환경에서 MDC trace_id/span_id를 Feign 요청 헤더로 전파하는 인터셉터.
 * OTel Agent 적용 환경에서는 W3C traceparent 헤더를 자동 주입하므로 이 인터셉터가 중복 역할을 하지 않는다.
 */
@Component
public class TraceIdRequestInterceptor implements RequestInterceptor {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SPAN_ID = "X-Span-Id";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("trace_id");
        if (traceId != null) {
            template.header(HEADER_TRACE_ID, traceId);
        }
        String spanId = MDC.get("span_id");
        if (spanId != null) {
            template.header(HEADER_SPAN_ID, spanId);
        }
    }
}
