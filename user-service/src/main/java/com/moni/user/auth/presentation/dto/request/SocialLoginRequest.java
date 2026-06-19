package com.moni.user.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginRequest {

    @NotBlank
    private String provider;

    @NotBlank
    private String code;

    @NotBlank
    private String codeVerifier;
}
