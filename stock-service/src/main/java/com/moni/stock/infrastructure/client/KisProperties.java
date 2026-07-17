package com.moni.stock.infrastructure.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

    private String appKey;
    private String appSecret;
    private String wsUrl;
    private String restUrl;
    private boolean mock;
    private int connectTimeout = 2000;
    private int readTimeout = 3000;
    private int connectionRequestTimeout = 1000;
    private int poolMaxTotal = 50;
    private int poolMaxPerRoute = 20;
    private int connectionTtl = 120000;
    private int validateAfterInactivity = 5000;
    private int evictIdleConnections = 30000;
    private int evictExpiredConnections = 30000;
}
