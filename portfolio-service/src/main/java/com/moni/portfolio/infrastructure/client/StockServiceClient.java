package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.StockResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "stock-service")
public interface StockServiceClient {

    @GetMapping("/api/v1/stocks/{ticker}")
    ExternalApiResponseDto<StockResponseDto> getStockDetail(
            @PathVariable("ticker") String ticker
    );
}
