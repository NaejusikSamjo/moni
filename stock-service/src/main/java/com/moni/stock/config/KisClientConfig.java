package com.moni.stock.config;

import com.moni.stock.infrastructure.client.KisProperties;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class KisClientConfig {

    private final KisProperties kisProperties;

    @Bean
    public RestClient kisRestClient() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig())
                .build();
        connectionManager.setMaxTotal(kisProperties.getPoolMaxTotal());
        connectionManager.setDefaultMaxPerRoute(kisProperties.getPoolMaxPerRoute());

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig())
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(kisProperties.getEvictIdleConnections()))
                .build();

        return RestClient.builder()
                .baseUrl(kisProperties.getRestUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    private ConnectionConfig connectionConfig() {
        return ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(kisProperties.getConnectTimeout()))
                .setSocketTimeout(Timeout.ofMilliseconds(kisProperties.getReadTimeout()))
                .setTimeToLive(TimeValue.ofMilliseconds(kisProperties.getConnectionTtl()))
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(kisProperties.getValidateAfterInactivity()))
                .build();
    }

    private RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(kisProperties.getConnectionRequestTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(kisProperties.getReadTimeout()))
                .build();
    }
}
