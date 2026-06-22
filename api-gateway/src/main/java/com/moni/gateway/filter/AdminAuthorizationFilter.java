package com.moni.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AdminAuthorizationFilter implements GlobalFilter, Ordered {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String ROLE_ADMIN = "ADMIN";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");

        if (!ROLE_ADMIN.equals(role)) {
            log.warn("[Gateway] 관리자 권한 없음 - path={}, role={}", path, role);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
