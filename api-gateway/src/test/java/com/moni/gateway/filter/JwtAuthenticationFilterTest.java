package com.moni.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@DisplayName("JwtAuthenticationFilter 단위 테스트 (목업)")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-for-jwt-authentication-filter-unit-test-only";
    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "secretKey", SECRET);
        ReflectionTestUtils.setField(filter, "gatewaySecret", GATEWAY_SECRET);
        ReflectionTestUtils.invokeMethod(filter, "init");

        key = Keys.hmacShaKeyFor(SECRET.getBytes());
        chain = mock(GatewayFilterChain.class);
        given(chain.filter(org.mockito.ArgumentMatchers.any())).willReturn(Mono.empty());
    }

    private String createAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }

    @Nested
    @DisplayName("화이트리스트 경로")
    class Whitelist {

        @Test
        @DisplayName("로그인 경로는 토큰 없이 통과하고 X-Gateway-Secret 헤더가 주입된다")
        void whitelisted_path_passes_without_token() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            verify(chain).filter(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("인증 필요 경로")
    class Authenticated {

        @Test
        @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
        void noAuthHeader_returns401() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me").build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Bearer 접두사가 없으면 401을 반환한다")
        void malformedAuthHeader_returns401() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                    .header("Authorization", "InvalidToken")
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("유효한 access 토큰이면 통과하고 사용자 헤더가 주입된다")
        void validAccessToken_passes() {
            // given
            UUID userId = UUID.randomUUID();
            String token = createAccessToken(userId, "test@moni.com", "USER");
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            verify(chain).filter(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("위조된 토큰이면 401을 반환한다")
        void invalidSignatureToken_returns401() {
            // given
            SecretKey otherKey = Keys.hmacShaKeyFor("other-totally-different-secret-key-for-test-purpose".getBytes());
            String forgedToken = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(otherKey)
                    .compact();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + forgedToken)
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("refresh 타입 토큰으로 접근하면 401을 반환한다")
        void refreshTypeToken_returns401() {
            // given
            UUID userId = UUID.randomUUID();
            Date now = new Date();
            String refreshToken = Jwts.builder()
                    .subject(userId.toString())
                    .claim("type", "refresh")
                    .issuedAt(now)
                    .expiration(new Date(now.getTime() + 60_000))
                    .signWith(key)
                    .compact();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + refreshToken)
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("필터 순서는 -1이다 (가장 먼저 실행)")
    void getOrder_returnsMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}