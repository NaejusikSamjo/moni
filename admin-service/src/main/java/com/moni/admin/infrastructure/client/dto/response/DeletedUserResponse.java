package com.moni.admin.infrastructure.client.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeletedUserResponse(
        UUID id,
        String email,
        String name,
        String nickname,
        String phone,
        String oauthProvider,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        String deletedReason
) {
}