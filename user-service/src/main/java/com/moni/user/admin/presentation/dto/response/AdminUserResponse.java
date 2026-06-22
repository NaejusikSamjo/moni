package com.moni.user.admin.presentation.dto.response;

import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.UserRole;
import com.moni.user.user.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String name,
        String nickname,
        String phone,
        String oauthProvider,
        UserRole role,
        UserStatus status,
        String suspendedReason,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getOauthProvider() != null ? user.getOauthProvider().name() : null,
                user.getRole(),
                user.getStatus(),
                user.getSuspendedReason(),
                user.getCreatedAt()
        );
    }
}
