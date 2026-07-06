package com.moni.trade.trade.infrastructure.client;

import com.moni.trade.global.config.FeignConfig;
import com.moni.trade.trade.infrastructure.client.dto.BatchStockRequestDto;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "stock-service", configuration = FeignConfig.class, primary = false)
public interface StockServiceClient {

    @GetMapping("/api/v1/stocks/{ticker}")
    ExternalApiResponseDto<StockPriceResponseDto> getStock(@PathVariable String ticker);

    @PostMapping("/api/v1/stocks/batch")
    ExternalApiResponseDto<List<StockPriceResponseDto>> getStocks(@RequestBody BatchStockRequestDto request);
}
