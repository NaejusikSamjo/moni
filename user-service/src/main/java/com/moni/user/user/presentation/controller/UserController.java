package com.moni.user.user.presentation.controller;

import com.moni.common.error.CommonErrorCode;
import com.moni.common.error.exception.CustomException;
import com.moni.common.response.GlobalResponse;
import com.moni.common.security.SecurityUtil;
import com.moni.user.user.application.service.UserService;
import com.moni.user.user.presentation.dto.request.InterestRequest;
import com.moni.user.user.presentation.dto.request.TendencyRequest;
import com.moni.user.user.presentation.dto.request.UserUpdateRequest;
import com.moni.user.user.presentation.dto.response.InterestResponse;
import com.moni.user.user.presentation.dto.response.TendencyResponse;
import com.moni.user.user.presentation.dto.response.UserResponse;
import com.moni.user.user.presentation.dto.response.WatchlistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<UserResponse>> getMe() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.getMe(userId)));
    }

    @Operation(summary = "내 정보 수정")
    @PatchMapping("/me")
    public ResponseEntity<GlobalResponse<UserResponse>> updateMe(@RequestBody @Valid UserUpdateRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.updateMe(userId, request)));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw() {
        UUID userId = currentUserId();
        userService.withdraw(userId, userId.toString());
        return ResponseEntity.noContent().build();
    }

    // 투자 성향

    @Operation(summary = "투자 성향 등록")
    @PostMapping("/me/tendency")
    public ResponseEntity<GlobalResponse<TendencyResponse>> createTendency(
            @RequestBody @Valid TendencyRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), userService.createTendency(userId, request)));
    }

    @Operation(summary = "투자 성향 조회")
    @GetMapping("/me/tendency")
    public ResponseEntity<GlobalResponse<TendencyResponse>> getTendency() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.getTendency(userId)));
    }

    @Operation(summary = "투자 성향 수정")
    @PutMapping("/me/tendency")
    public ResponseEntity<GlobalResponse<TendencyResponse>> updateTendency(
            @RequestBody @Valid TendencyRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.updateTendency(userId, request)));
    }

    // 관심사

    @Operation(summary = "관심사 등록")
    @PostMapping("/me/interests")
    public ResponseEntity<GlobalResponse<List<InterestResponse>>> createInterests(
            @RequestBody @Valid InterestRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), userService.createInterests(userId, request)));
    }

    @Operation(summary = "관심사 조회")
    @GetMapping("/me/interests")
    public ResponseEntity<GlobalResponse<List<InterestResponse>>> getInterests() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.getInterests(userId)));
    }

    @Operation(summary = "관심사 수정")
    @PutMapping("/me/interests")
    public ResponseEntity<GlobalResponse<List<InterestResponse>>> updateInterests(
            @RequestBody @Valid InterestRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.updateInterests(userId, request)));
    }

    // 관심종목

    @Operation(summary = "관심종목 추가")
    @PutMapping("/me/watchlist/{stockCode}")
    public ResponseEntity<GlobalResponse<WatchlistResponse>> addWatchlist(@PathVariable String stockCode) {
        UUID userId = currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), userService.addWatchlist(userId, stockCode)));
    }

    @Operation(summary = "관심종목 목록 조회")
    @GetMapping("/me/watchlist")
    public ResponseEntity<GlobalResponse<List<WatchlistResponse>>> getWatchlist() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), userService.getWatchlist(userId)));
    }

    @Operation(summary = "관심종목 삭제")
    @DeleteMapping("/me/watchlist/{stockCode}")
    public ResponseEntity<Void> removeWatchlist(@PathVariable String stockCode) {
        UUID userId = currentUserId();
        userService.removeWatchlist(userId, stockCode);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new CustomException(CommonErrorCode.UNAUTHORIZED));
    }
}
