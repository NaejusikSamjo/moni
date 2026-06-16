package com.moni.user.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;


/**
 * 게이트웨이를 통하지 않은 직접 접근을 차단하는 필터.
 *
 * <p>보안 목적:
 * <ul>
 *   <li>외부 클라이언트가 서비스 포트에 직접 접근하는 것을 차단</li>
 *   <li>같은 Docker 네트워크 내 다른 서비스가 게이트웨이를 우회하여
 *       임의의 {@code X-User-Id}를 주입한 채 직접 호출하는 것을 차단</li>
 * </ul>
 *
 * <p>게이트웨이는 JWT 검증 후 {@code X-Gateway-Secret} 헤더를 주입하며,
 * 이 필터는 해당 시크릿이 없는 요청을 401로 거부한다.
 */
@Slf4j
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (SKIP_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String receivedSecret = request.getHeader(GATEWAY_SECRET_HEADER);
        if (!gatewaySecret.equals(receivedSecret)) {
            log.warn("[GatewayAuthFilter] X-Gateway-Secret 불일치 - path={}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
