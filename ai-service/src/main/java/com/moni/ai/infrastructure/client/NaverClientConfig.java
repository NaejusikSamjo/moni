package com.moni.ai.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class NaverClientConfig {

    @Value("${naver.news.base-url}")
    private String newsBaseUrl;

    @Value("${naver.news.client-id}")
    private String newsClientId;

    @Value("${naver.news.client-secret}")
    private String newsClientSecret;

    @Bean
    public RestClient naverNewsRestClient() {
        return RestClient.builder()
                .baseUrl(newsBaseUrl)
                .defaultHeader("X-Naver-Client-Id",newsClientId)
                .defaultHeader("X-Naver-Client-Secret", newsClientSecret)
                .build();
    }
}
