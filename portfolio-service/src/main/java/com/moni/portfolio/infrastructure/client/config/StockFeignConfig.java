package com.moni.portfolio.infrastructure.client.config;

import com.moni.common.security.SecurityUtil;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class StockFeignConfig {

    @Bean
    public RequestInterceptor stockRequestInterceptor(
            @Value("${gateway.secret}") String gatewaySecret
    ) {
        return template -> {
            template.header("X-Gateway-Secret", gatewaySecret);
            SecurityUtil.getCurrentUserId()
                    .ifPresent(userId -> template.header("X-User-Id", userId.toString()));
            SecurityUtil.getCurrentUserRole()
                    .filter(role -> !role.isBlank())
                    .ifPresent(role -> template.header("X-User-Role", role));
        };
    }
}
