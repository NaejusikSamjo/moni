package com.moni.portfolio.infrastructure.client.config;

import com.moni.common.security.SecurityUtil;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class FeignConfig {

    @Bean
    public RequestInterceptor stockRequestInterceptor(
            @Value("${gateway.secret}") String gatewaySecret
    ) {
        return template -> {
            template.header("X-Gateway-Secret", gatewaySecret);
            if (!template.headers().containsKey("X-User-Id")) {
                SecurityUtil.getCurrentUserId()
                        .ifPresent(userId -> template.header("X-User-Id", userId.toString()));
            }
            if (!template.headers().containsKey("X-User-Role")) {
                SecurityUtil.getCurrentUserRole()
                        .filter(role -> !role.isBlank())
                        .ifPresent(role -> template.header("X-User-Role", role));
            }
        };
    }
}
