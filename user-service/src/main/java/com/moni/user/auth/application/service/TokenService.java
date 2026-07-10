package com.moni.user.auth.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.domain.exception.TokenErrorCode;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.global.jwt.JwtUtil;
import com.moni.user.global.redis.CasResult;
import com.moni.user.global.redis.RedisService;
import com.moni.user.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    // Refresh Token Redis 저장 (login/refresh에서 재사용)
    public void saveRefreshToken(UUID userId, String refreshToken) {
        redisService.set(
                jwtUtil.getRefreshTokenKey(userId),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );
    }

    // JWT 생성 + Refresh Token Redis 저장 + 응답 반환 (login/refresh에서 재사용)
    public LoginResponse issueTokens(User user) {
        String accessToken = jwtUtil.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    // Refresh Token 검증 + Redis 비교 → userId 반환 (logout 등 원자성이 필요 없는 조회 전용 경로)
    public UUID validateAndGetUserId(String refreshToken) {
        UUID userId = getVerifiedUserId(refreshToken);

        String stored = redisService.get(jwtUtil.getRefreshTokenKey(userId));
        if (stored == null) {
            throw new CustomException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!refreshToken.equals(stored)) {
            throw new CustomException(TokenErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        return userId;
    }

    // JWT 서명/만료만 검증 (Redis 조회 없음) — refresh 흐름에서 원자적 rotate 전 userId 확보용
    public UUID getVerifiedUserId(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(TokenErrorCode.INVALID_REFRESH_TOKEN);
        }
        return jwtUtil.getUserId(refreshToken);
    }

    // 새 토큰 발급 + Redis의 기존 refresh token을 원자적 교체 (동시 refresh 시 재사용/중복 발급 방지)
    public LoginResponse rotateRefreshToken(User user, String oldRefreshToken) {
        String newAccessToken = jwtUtil.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtil.createRefreshToken(user.getId());

        CasResult result = redisService.compareAndSet(
                jwtUtil.getRefreshTokenKey(user.getId()),
                oldRefreshToken,
                newRefreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );

        if (result == CasResult.NOT_FOUND) {
            throw new CustomException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (result == CasResult.MISMATCH) {
            throw new CustomException(TokenErrorCode.REFRESH_TOKEN_REUSED);
        }

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    // Access Token 블랙리스트 등록 (refresh/logout에서 재사용)
    public void blacklistAccessToken(String accessToken, String reason) {
        try {
            long expirationMillis =
                    jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();

            if (expirationMillis > 0) {
                redisService.set(
                        jwtUtil.getAccessTokenBlacklistKey(accessToken),
                        reason,
                        Duration.ofMillis(expirationMillis)
                );
            }
        } catch (Exception e) {
            log.warn("[AUTH] {} 중 Access Token 블랙리스트 등록 실패 - {}", reason, e.getMessage());
        }
    }

    // Refresh Token Redis 삭제
    public void deleteRefreshToken(UUID userId) {
        redisService.delete(jwtUtil.getRefreshTokenKey(userId));
    }

    public void deleteRefreshTokenByToken(String refreshToken) {
        try {
            UUID userId = jwtUtil.getUserId(refreshToken);
            deleteRefreshToken(userId);
        } catch (Exception e) {
            log.warn("[AUTH] logout 중 Refresh Token 삭제 실패 - {}", e.getMessage());
        }
    }
}