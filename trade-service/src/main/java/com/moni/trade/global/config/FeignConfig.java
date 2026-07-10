package com.moni.trade.global.config;

import com.moni.common.security.SecurityUtil;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public RequestInterceptor gatewaySecretInterceptor() {
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
