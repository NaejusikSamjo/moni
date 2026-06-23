package com.moni.admin.infrastructure.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class AdminUserResponse {

    private UUID id;
    private String email;
    private String name;
    private String nickname;
    private String phone;
    private String oauthProvider;
    private String role;
    private String status;
    private String suspendedReason;
    private LocalDateTime createdAt;
}