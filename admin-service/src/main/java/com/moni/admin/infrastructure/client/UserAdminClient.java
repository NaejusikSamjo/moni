package com.moni.admin.infrastructure.client;

import com.moni.admin.global.config.feign.AdminFeignConfig;
import com.moni.admin.infrastructure.client.dto.response.AdminUserResponse;
import com.moni.admin.infrastructure.client.dto.response.DeletedUserResponse;
import com.moni.admin.infrastructure.client.dto.response.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;


@FeignClient(name = "user-service", contextId = "userAdminClient", configuration = AdminFeignConfig.class)
public interface UserAdminClient {

    @GetMapping("/api/v1/admin/users")
    PageResponse<AdminUserResponse> getUsers(@SpringQueryMap Pageable pageable);

    @GetMapping("/api/v1/admin/users/deleted")
    PageResponse<DeletedUserResponse> getDeletedUsers(@SpringQueryMap Pageable pageable);

    @PatchMapping("/api/v1/admin/users/{userId}/suspend")
    void suspend(@PathVariable UUID userId, @RequestBody Map<String, String> body);

    @PatchMapping("/api/v1/admin/users/{userId}/unsuspend")
    void unsuspend(@PathVariable UUID userId);

    @DeleteMapping("/api/v1/admin/users/{userId}")
    void delete(@PathVariable UUID userId);
}