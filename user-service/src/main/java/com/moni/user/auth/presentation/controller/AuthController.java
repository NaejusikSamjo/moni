package com.moni.user.auth.presentation.controller;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.GlobalResponse;
import com.moni.user.auth.application.service.AuthService;
import com.moni.user.auth.domain.exception.TokenErrorCode;
import com.moni.user.auth.presentation.dto.request.LoginRequest;
import com.moni.user.auth.presentation.dto.request.SignupRequest;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.auth.presentation.dto.response.SignupResponse;
import com.moni.user.global.util.NicknameGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final NicknameGenerator nicknameGenerator;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<GlobalResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        String nickname = nicknameGenerator.generate();
        SignupResponse response = authService.signup(request, nickname);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<LoginResponse>> refresh(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = extractAccessToken(accessHeader);
        validateRefreshToken(refreshToken);
        return ResponseEntity.ok(
                GlobalResponse.success(HttpStatus.OK.value(), authService.refresh(accessToken, refreshToken)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = extractAccessToken(accessHeader);
        validateRefreshToken(refreshToken);
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }

    private String extractAccessToken(String accessHeader) {
        if (accessHeader == null || !accessHeader.startsWith("Bearer ")) {
            throw new CustomException(TokenErrorCode.INVALID_REFRESH_TOKEN);
        }
        return accessHeader.substring(7);
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(TokenErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}