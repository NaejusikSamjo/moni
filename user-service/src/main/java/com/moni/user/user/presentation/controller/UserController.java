package com.moni.user.user.presentation.controller;

import com.moni.common.error.CommonErrorCode;
import com.moni.common.error.exception.CustomException;
import com.moni.common.response.GlobalResponse;
import com.moni.common.security.SecurityUtil;
import com.moni.user.user.application.service.UserService;
import com.moni.user.user.presentation.dto.request.ChangePasswordRequest;
import com.moni.user.user.presentation.dto.request.InterestRequest;
import com.moni.user.user.presentation.dto.request.ProfileUpdateRequest;
import com.moni.user.user.presentation.dto.request.IntegrateRequest;
import com.moni.user.user.presentation.dto.request.TendencyRequest;
import com.moni.user.user.presentation.dto.request.UserUpdateRequest;
import com.moni.user.user.presentation.dto.request.WithdrawRequest;
import com.moni.user.user.presentation.dto.response.InterestResponse;
import com.moni.user.user.presentation.dto.response.PresignedUrlResponse;
import com.moni.user.user.presentation.dto.response.TendencyResponse;
import com.moni.user.user.presentation.dto.response.UserResponse;
import com.moni.user.user.presentation.dto.response.WatchlistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
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
@Validated
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

    @Operation(summary = "통합 회원 전환 (OAuth 가입 유저 전용)")
    @PostMapping("/me/integrate")
    public ResponseEntity<Void> integrate(@RequestBody @Valid IntegrateRequest request) {
        UUID userId = currentUserId();
        userService.integrate(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경")
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        UUID userId = currentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴 (비밀번호 설정 유저는 비밀번호 검증 필요)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@RequestBody(required = false) WithdrawRequest request) {
        UUID userId = currentUserId();
        userService.withdraw(userId, request != null ? request : new WithdrawRequest(), userId.toString());
        return ResponseEntity.noContent().build();
    }

    // 프로필 이미지

    @Operation(summary = "프로필 업로드용 Presigned URL 발급")
    @GetMapping("/me/profile/presigned-url")
    public ResponseEntity<GlobalResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam @Pattern(regexp = "^(jpg|jpeg|png|webp)$",
                    message = "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, webp)") String extension) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(),
                userService.getPresignedUrl(userId, extension)));
    }

    @Operation(summary = "프로필 수정 (이모지 또는 S3 URL)")
    @PatchMapping("/me/profile")
    public ResponseEntity<GlobalResponse<UserResponse>> updateProfileImage(
            @RequestBody @Valid ProfileUpdateRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(),
                userService.updateProfileImage(userId, request)));
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
    public ResponseEntity<GlobalResponse<WatchlistResponse>> addWatchlist(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "종목코드는 영문/숫자 6자리여야 합니다.") String stockCode) {
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
    public ResponseEntity<Void> removeWatchlist(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "종목코드는 영문/숫자 6자리여야 합니다.") String stockCode) {
        UUID userId = currentUserId();
        userService.removeWatchlist(userId, stockCode);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new CustomException(CommonErrorCode.UNAUTHORIZED));
    }
}
