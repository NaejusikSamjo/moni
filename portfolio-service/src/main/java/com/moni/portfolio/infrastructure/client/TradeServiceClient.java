package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.config.FeignConfig;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingsResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "trade-service", configuration = FeignConfig.class)
public interface TradeServiceClient {

    @GetMapping("/api/v1/assets")
    ExternalApiResponseDto<TradeAssetResponseDto> getAssets(
            @RequestHeader("X-User-Id") UUID userId
    );

    @GetMapping("/api/v1/assets/holdings")
    ExternalApiResponseDto<TradeAssetHoldingsResponseDto> getAssetHoldings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") String sort
    );
}
