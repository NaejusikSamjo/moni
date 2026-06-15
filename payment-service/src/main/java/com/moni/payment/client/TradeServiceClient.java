package com.moni.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "trade-service")
public interface TradeServiceClient {

    @GetMapping("/api/trades/{tradeId}")
    String getTrade(@PathVariable("tradeId") String tradeId);
}
