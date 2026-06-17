package com.moni.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            // OTel Agent가 MDC에 trace_id/span_id를 주입하므로, 미적용 환경을 위한 fallback
            if (MDC.get("trace_id") == null) {
                MDC.put("trace_id", UUID.randomUUID().toString().replace("-", ""));
            }
            if (MDC.get("span_id") == null) {
                MDC.put("span_id", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
