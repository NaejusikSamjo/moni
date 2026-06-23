package com.moni.user.admin.presentation.dto.response;

import com.moni.user.user.domain.entity.User;

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
    public static DeletedUserResponse from(User user) {
        return new DeletedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getOauthProvider() != null ? user.getOauthProvider().name() : null,
                user.getCreatedAt(),
                user.getDeletedAt(),
                user.getDeletedReason()
        );
    }
}
