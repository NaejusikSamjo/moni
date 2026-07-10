package com.moni.user.admin.presentation.controller;

import com.moni.user.admin.application.service.AdminService;
import com.moni.user.admin.presentation.dto.request.SuspendRequest;
import com.moni.user.admin.presentation.dto.response.AdminUserResponse;
import com.moni.user.admin.presentation.dto.response.DeletedUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin User API", description = "admin-service Feign 전용 내부 API (X-Gateway-Secret 필수)")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "유저 목록 조회", description = "가입일 내림차순 페이징")
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getUsers(pageable));
    }

    @Operation(summary = "삭제된 유저 목록 조회")
    @GetMapping("/deleted")
    public ResponseEntity<Page<DeletedUserResponse>> getDeletedUsers(
            @PageableDefault(sort = "deletedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getDeletedUsers(pageable));
    }

    @Operation(summary = "유저 계정 정지")
    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspend(
            @PathVariable UUID userId,
            @RequestBody @Valid SuspendRequest request) {
        adminService.suspendUser(userId, request.getReason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "유저 계정 정지 해제")
    @PatchMapping("/{userId}/unsuspend")
    public ResponseEntity<Void> unsuspend(@PathVariable UUID userId) {
        adminService.unsuspendUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "유저 계정 삭제", description = "소프트 삭제")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String adminId) {
        adminService.deleteUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }

}
