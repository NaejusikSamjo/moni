package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.config.FeignConfig;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetAnalysisSnapshotResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "trade-service", configuration = FeignConfig.class)
public interface TradeServiceClient {

    @GetMapping("/api/v1/assets/analysis-snapshot")
    ExternalApiResponseDto<TradeAssetAnalysisSnapshotResponseDto> getAnalysisSnapshot(
            @RequestHeader("X-User-Id") UUID userId
    );
}
