package com.moni.payment.presentation.controller;

import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.presentation.dto.SubscriptionStatusResponse;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.application.usecase.GetSubscriptionStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final GetSubscriptionStatusUseCase getSubscriptionStatusUseCase;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getStatus(
            @RequestHeader("X-User-Id") UUID userId) {
        Subscription subscription = getSubscriptionStatusUseCase.getSubscriptionStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(SubscriptionStatusResponse.from(subscription)));
    }
}
