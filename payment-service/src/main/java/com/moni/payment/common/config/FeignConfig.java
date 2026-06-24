package com.moni.payment.common.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    private static final long CONNECT_TIMEOUT_SECONDS = 2L;
    private static final long READ_TIMEOUT_SECONDS = 5L;

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                READ_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                true
        );
    }
}
