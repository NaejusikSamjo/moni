package com.moni.trade.trade.infrastructure.client;

import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "stock-service")
public interface StockServiceClient {

    @GetMapping("/api/v1/stocks/{ticker}")
    StockPriceResponseDto getStock(@PathVariable String ticker);
}
