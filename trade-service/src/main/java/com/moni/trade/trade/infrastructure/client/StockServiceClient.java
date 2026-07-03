package com.moni.trade.trade.infrastructure.client;

import com.moni.trade.global.config.FeignConfig;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "stock-service", configuration = FeignConfig.class, primary = false)
public interface StockServiceClient {

    @GetMapping("/api/v1/stocks/{ticker}")
    ExternalApiResponseDto<StockPriceResponseDto> getStock(@PathVariable String ticker);
}
