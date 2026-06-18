package com.moni.ai.infrastructure.client;

import com.moni.ai.presentation.dto.response.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NaverNewsClient {

    private final RestClient naverNewsRestClient;


    public List<NaverNewsResponse.NaverNewsItem> fetchNews(String query, int display,String sort) {


        NaverNewsResponse response = naverNewsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", display)   // 최대 100
                        .queryParam("sort", sort)       // 최신순
                        .build())
                .retrieve()
                .body(NaverNewsResponse.class);

        return response != null ? response.getItems() : List.of();
    }



}
