package com.moni.user.auth.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.user.auth.application.service.SocialLoginService;
import com.moni.user.auth.presentation.dto.request.SocialLoginRequest;
import com.moni.user.auth.presentation.dto.request.SocialLoginUrlRequest;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.auth.presentation.dto.response.SocialLoginUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SocialLogin", description = "소셜 로그인 API (Authorization Code + PKCE)")
@RestController
@RequestMapping("/api/v1/auth/social")
@RequiredArgsConstructor
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    @Operation(summary = "소셜 로그인 URL 생성")
    @PostMapping("/login-url")
    public ResponseEntity<GlobalResponse<SocialLoginUrlResponse>> getLoginUrl(
            @RequestBody @Valid SocialLoginUrlRequest request) {
        String loginUrl = socialLoginService.getLoginUrl(
                request.getProvider(), request.getCodeChallenge(), request.getState());
        return ResponseEntity.ok(
                GlobalResponse.success(HttpStatus.OK.value(), new SocialLoginUrlResponse(loginUrl)));
    }

    @Operation(summary = "소셜 로그인")
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<LoginResponse>> login(@RequestBody @Valid SocialLoginRequest request) {
        LoginResponse response = socialLoginService.login(
                request.getProvider(), request.getCode(), request.getCodeVerifier());
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
