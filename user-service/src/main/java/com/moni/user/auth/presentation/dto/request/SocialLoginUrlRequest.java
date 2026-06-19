package com.moni.user.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginUrlRequest {

    @NotBlank
    private String provider;

    @NotBlank
    private String codeChallenge;

    @NotBlank
    private String state;
}
