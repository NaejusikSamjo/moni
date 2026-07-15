package com.moni.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 게이트웨이 진입 시점에 JWT Access Token 검증 후,
 * 검증된 사용자 정보를 X-User-Id / X-User-Email / X-User-Role 헤더로 변환하여
 * 하위 마이크로서비스로 전달하는 전역 필터.
 *
 * <p><b>X-User-Role 관련 참고</b><br>
 * 현재 UserRole은 USER 단일 값만 존재하며, 하위 서비스 어디에도
 * role 기반 인가(@PreAuthorize, hasRole 등) 로직 사용 없음.
 * 유저 role을 분리해서 관리하던 이전 구조에서 리팩토링 중 남은 방식으로,
 * X-User-Role 헤더는 현재 실질적인 권한 분기에는 사용하지 않음.
 *
 * <p>어드민 기능은 이 필터의 인증 경로를 타지 않고 Okta(OIDC) 기반으로
 * 별도 인증하며, admin-service에서 인증된 관리자 정보를 신뢰 가능한
 * X-User-Id로 변환해 내부 서비스에 전달함. 인증 방식 자체는 어드민 포함
 * 전부 토큰(JWT) 기반이며, role 헤더는 그 흐름과 무관함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BLACKLIST_PREFIX = "blacklist:access:";

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private final ReactiveStringRedisTemplate redisTemplate;

    private SecretKey key;

    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/refresh",
            "/api/v1/auth/social",
            "/swagger-ui",
            "/v3/api-docs",
            "/user-service/v3/api-docs",
            "/trade-service/v3/api-docs",
            "/stock-service/v3/api-docs",
            "/portfolio-service/v3/api-docs",
            "/notification-service/v3/api-docs",
            "/payment-service/v3/api-docs",
            "/ai-service/v3/api-docs",
            "/actuator",
            "/toss-billing-test.html", "/toss-success.html", "/toss-fail.html"
    );

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 요청 경로가 화이트리스트가 아니면 JWT를 검증하고,
     * 검증 결과(userId, email, role)를 신뢰 헤더로 하위 서비스에 전달함.
     *
     * <p>role은 UserRole.USER 단일 값으로 운영 중이며, 이전 구조에서
     * 리팩토링 중 남은 방식으로 현재는 사용하지 않음. 어드민 인증은
     * Okta로 별도 분리되어 이 경로를 타지 않음.
     *
     * @param exchange 현재 요청/응답 컨텍스트
     * @param chain    다음 필터 체인
     * @return 필터 처리 완료 Mono
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        if (isWhitelisted(path)) {
            ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                @NonNull
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(super.getHeaders());
                    stripTrustedHeaders(headers);
                    headers.set("X-Gateway-Secret", gatewaySecret);
                    return headers;
                }
            };
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                log.warn("[Gateway] access 토큰이 아님 - type={}", type);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);

            /*
             * role은 UserRole.USER 단일 값만 존재함 (관리자 role 없음, 관리자 인증은 Okta로 분리됨).
             * 유저 role을 분리해서 관리하던 이전 구조에서 리팩토링 중 남은 방식으로,
             * 하위 서비스에서 role 기반 인가 로직에 사용하지 않음.
             * 아래 X-User-Role 헤더는 현재 사용하지 않는 참고용 값임.
             */
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                @NonNull
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(super.getHeaders());
                    stripTrustedHeaders(headers); // 클라이언트가 위조해 보낸 인증 헤더 제거 (덮어쓰기 전 방어)
                    headers.set("X-Gateway-Secret", gatewaySecret);
                    headers.set("X-User-Id", userId);
                    headers.set("X-User-Email", email);
                    headers.set("X-User-Role", role != null ? role : "");
                    return headers;
                }
            };

            return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.warn("[Gateway] 블랙리스트 토큰 차단 - userId={}", userId);
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });

        } catch (JwtException e) {
            log.warn("[Gateway] JWT 검증 실패: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 클라이언트가 보낸 요청에 이미 X-User-Id / X-User-Role 등 신뢰 헤더가 섞여 들어와도
     * 게이트웨이가 새로 세팅하기 전에 전부 제거함 (위조 헤더 통과 방지).
     */
    private void stripTrustedHeaders(HttpHeaders headers) {
        headers.remove("X-Gateway-Secret");
        headers.remove("X-User-Id");
        headers.remove("X-User-Email");
        headers.remove("X-User-Role");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}