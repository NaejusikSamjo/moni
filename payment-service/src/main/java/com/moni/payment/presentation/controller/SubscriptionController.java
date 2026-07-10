package com.moni.payment.presentation.controller;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.application.dto.command.CancelSubscriptionResult;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.ReactivateSubscriptionUseCase;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.application.service.SubscribeCancelUseCase;
import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.presentation.dto.CancelSubscriptionResponse;
import com.moni.payment.presentation.dto.SubscribeResponse;
import com.moni.payment.presentation.dto.SubscriptionStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscribeCancelUseCase subscribeCancelUsecase;
    private final ReactivateSubscriptionUseCase reactivateSubscriptionUseCase;

    @Operation(summary = "현재 구독 상태를 조회합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getStatus(
            @RequestHeader("X-User-Id") UUID userId) {
        SubscriptionStatusResult result = subscriptionQueryService.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(SubscriptionStatusResponse.from(result)));
    }

    @Operation(summary = "현재 구독중이라면 구독을 중지합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<CancelSubscriptionResponse>> cancelSubscription(
            @RequestHeader("X-User-Id") UUID userId) {
        CancelSubscriptionResult result = subscribeCancelUsecase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(CancelSubscriptionResponse.from(result)));
    }

    @Operation(summary = "SUSPENDED 상태인 구독을 재활성화합니다.")
    @PostMapping("/reactivate")
    public ResponseEntity<ApiResponse<SubscribeResponse>> reactivateSubscription(
            @RequestHeader("X-User-Id") UUID userId) {
        SubscribeResult result = reactivateSubscriptionUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(SubscribeResponse.from(result)));
    }
}
