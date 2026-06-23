package com.moni.admin.application.service;

import com.moni.admin.infrastructure.client.UserAdminClient;
import com.moni.admin.infrastructure.client.dto.response.AdminUserResponse;
import com.moni.admin.infrastructure.client.dto.response.DeletedUserResponse;
import com.moni.admin.infrastructure.client.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminFacadeService {

    private final UserAdminClient userAdminClient;

    public PageResponse<AdminUserResponse> getUsers(Pageable pageable) {
        return userAdminClient.getUsers(pageable);
    }

    public PageResponse<DeletedUserResponse> getDeletedUsers(Pageable pageable) {
        return userAdminClient.getDeletedUsers(pageable);
    }

    public void suspendUser(UUID userId, String reason) {
        userAdminClient.suspend(userId, Map.of("reason", reason));
    }

    public void unsuspendUser(UUID userId) {
        userAdminClient.unsuspend(userId);
    }

    public void deleteUser(UUID userId) {
        userAdminClient.delete(userId);
    }

}
