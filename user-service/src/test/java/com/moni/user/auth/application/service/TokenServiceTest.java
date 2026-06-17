package com.moni.user.auth.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.domain.exception.TokenErrorCode;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.global.jwt.JwtUtil;
import com.moni.user.global.redis.RedisService;
import com.moni.user.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 단위 테스트 (목업)")
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private TokenService tokenService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockUser = User.create("test@moni.com", "encodedPassword", "홍길동", "귀여운 주니어#a1b2c3", "010-1234-5678");
        userId = mockUser.getId();
    }

    @Nested
    @DisplayName("토큰 발급")
    class IssueTokens {

        @Test
        @DisplayName("정상적으로 access/refresh 토큰을 발급하고 Redis에 저장한다")
        void issueTokens_success() {
            // given
            given(jwtUtil.createAccessToken(any(), any(), any())).willReturn("access-token");
            given(jwtUtil.createRefreshToken(any())).willReturn("refresh-token");
            given(jwtUtil.getRefreshTokenKey(any())).willReturn("refresh:" + userId);
            given(jwtUtil.getRefreshTokenExpiration()).willReturn(2592000000L);

            // when
            LoginResponse response = tokenService.issueTokens(mockUser);

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            verify(redisService).set(eq("refresh:" + userId), eq("refresh-token"), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 검증")
    class ValidateRefreshToken {

        @Test
        @DisplayName("유효하고 Redis 값과 일치하면 userId를 반환한다")
        void validate_success() {
            // given
            given(jwtUtil.validateToken("refresh-token")).willReturn(true);
            given(jwtUtil.getUserId("refresh-token")).willReturn(userId);
            given(jwtUtil.getRefreshTokenKey(userId)).willReturn("refresh:" + userId);
            given(redisService.get("refresh:" + userId)).willReturn("refresh-token");

            // when
            UUID result = tokenService.validateAndGetUserId("refresh-token");

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 예외를 던진다")
        void validate_fail_invalidToken() {
            // given
            given(jwtUtil.validateToken("invalid-token")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> tokenService.validateAndGetUserId("invalid-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TokenErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Redis에 토큰이 없으면 예외를 던진다")
        void validate_fail_notFound() {
            // given
            given(jwtUtil.validateToken("refresh-token")).willReturn(true);
            given(jwtUtil.getUserId("refresh-token")).willReturn(userId);
            given(jwtUtil.getRefreshTokenKey(userId)).willReturn("refresh:" + userId);
            given(redisService.get("refresh:" + userId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> tokenService.validateAndGetUserId("refresh-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("Redis 값과 토큰이 불일치하면 예외를 던진다")
        void validate_fail_mismatch() {
            // given
            given(jwtUtil.validateToken("refresh-token")).willReturn(true);
            given(jwtUtil.getUserId("refresh-token")).willReturn(userId);
            given(jwtUtil.getRefreshTokenKey(userId)).willReturn("refresh:" + userId);
            given(redisService.get("refresh:" + userId)).willReturn("other-token");

            // when & then
            assertThatThrownBy(() -> tokenService.validateAndGetUserId("refresh-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TokenErrorCode.REFRESH_TOKEN_MISMATCH);
        }
    }

    @Nested
    @DisplayName("Access Token 블랙리스트")
    class BlacklistAccessToken {

        @Test
        @DisplayName("만료 전 토큰이면 블랙리스트에 등록한다")
        void blacklist_success() {
            // given
            given(jwtUtil.getExpiration("access-token")).willReturn(new Date(System.currentTimeMillis() + 60000));
            given(jwtUtil.getAccessTokenBlacklistKey("access-token")).willReturn("blacklist:access:access-token");

            // when
            tokenService.blacklistAccessToken("access-token", "logout");

            // then
            verify(redisService).set(eq("blacklist:access:access-token"), eq("logout"), any(Duration.class));
        }

        @Test
        @DisplayName("이미 만료된 토큰이면 블랙리스트에 등록하지 않는다")
        void blacklist_skip_expired() {
            // given
            given(jwtUtil.getExpiration("access-token")).willReturn(new Date(System.currentTimeMillis() - 60000));

            // when
            tokenService.blacklistAccessToken("access-token", "logout");

            // then
            verify(redisService, org.mockito.Mockito.never()).set(anyString(), anyString(), any(Duration.class));
        }
    }
}