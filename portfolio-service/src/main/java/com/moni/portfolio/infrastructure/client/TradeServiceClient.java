package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.dto.response.TradeAccountResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradePageResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "trade-service")
public interface TradeServiceClient {

    @GetMapping("/api/v1/accounts")
    ExternalApiResponseDto<TradeAccountResponseDto> getAccount(
            @RequestHeader("X-User-Id") UUID userId
    );

    @GetMapping("/api/v1/holdings")
    ExternalApiResponseDto<TradePageResponseDto<TradeHoldingResponseDto>> getHoldings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}
