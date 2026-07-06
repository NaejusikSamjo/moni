package com.moni.admin.global.config.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class StockAdminFeignConfig {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public RequestInterceptor stockAdminRequestInterceptor() {
        return template -> {
            template.header("X-Gateway-Secret", gatewaySecret);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                template.header("X-User-Id", auth.getName());
            }
        };
    }
}
