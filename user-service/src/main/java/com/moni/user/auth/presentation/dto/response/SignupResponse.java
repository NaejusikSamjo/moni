package com.moni.user.auth.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class SignupResponse {

    private UUID id;
    private String email;
    private String nickname;
}