package com.moni.payment.presentation.controller;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.application.usecase.GetSubscriptionStatusQuery;
import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.presentation.dto.SubscriptionStatusResponse;
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

    private final GetSubscriptionStatusQuery getSubscriptionStatusQuery;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getStatus(
            @RequestHeader("X-User-Id") UUID userId) {
        SubscriptionStatusResult result = getSubscriptionStatusQuery.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(SubscriptionStatusResponse.from(result)));
    }
}
