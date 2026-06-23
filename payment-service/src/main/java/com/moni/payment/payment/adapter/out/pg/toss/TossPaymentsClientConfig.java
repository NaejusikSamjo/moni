package com.moni.payment.payment.adapter.out.pg.toss;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class TossPaymentsClientConfig {

    private static final long TOSS_CONNECT_TIMEOUT_SECONDS = 3L;
    private static final long TOSS_READ_TIMEOUT_SECONDS = 10L;

    @Value("${toss.payments.secret-key:test_sk_placeholder}")
    private String secretKey;

    @Bean
    public RequestInterceptor tossAuthInterceptor() {
        return requestTemplate -> {
            String credentials = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
            requestTemplate.header("Authorization", "Basic " + credentials);
        };
    }

    @Bean
    public Request.Options tossRequestOptions() {
        return new Request.Options(
                TOSS_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                TOSS_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                true);
    }
}
