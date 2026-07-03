package com.moni.stock.config;

import com.moni.stock.infrastructure.client.KisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class KisClientConfig {

    private final KisProperties kisProperties;

    @Bean
    public RestClient kisRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(kisProperties.getConnectTimeout()))
                .withReadTimeout(Duration.ofMillis(kisProperties.getReadTimeout()));

        return RestClient.builder()
                .baseUrl(kisProperties.getRestUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
