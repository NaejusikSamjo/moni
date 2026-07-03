package com.moni.admin.infrastructure.client;

import com.moni.admin.global.config.feign.AiAdminFeignConfig;
import com.moni.admin.infrastructure.client.dto.request.NewsCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", contextId = "aiAdminClient", configuration = AiAdminFeignConfig.class)
public interface AiAdminClient {

    @PostMapping("/api/v1/admin/ai/news/fetch")
    void fetchAllNews();

    @PostMapping("/api/v1/admin/ai/news/market/fetch")
    void fetchMarketNews();

    @PostMapping("/api/v1/admin/ai/news/ticker")
    void createNews(@RequestBody NewsCreateRequest request);
}
