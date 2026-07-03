package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.config.FeignConfig;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.SubscriptionStatusResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "payment-service", configuration = FeignConfig.class)
public interface PaymentServiceClient {

    @GetMapping("/api/v1/payments/subscriptions/status")
    ExternalApiResponseDto<SubscriptionStatusResponseDto> getSubscriptionStatus(
            @RequestHeader("X-User-Id") UUID userId
    );
}
