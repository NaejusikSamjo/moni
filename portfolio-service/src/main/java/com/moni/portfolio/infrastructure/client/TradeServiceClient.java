package com.moni.portfolio.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "trade-service")
public interface TradeServiceClient {
}
