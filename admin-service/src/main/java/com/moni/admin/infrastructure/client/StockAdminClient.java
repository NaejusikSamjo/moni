package com.moni.admin.infrastructure.client;

import com.moni.admin.global.config.feign.StockAdminFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "stock-service", contextId = "stockAdminClient", configuration = StockAdminFeignConfig.class)
public interface StockAdminClient {

    @PostMapping("/api/v1/admin/stocks/download/stocks")
    void downloadStocks();
}
