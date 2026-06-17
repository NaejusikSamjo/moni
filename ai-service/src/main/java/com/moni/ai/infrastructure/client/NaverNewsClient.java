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

        // TODO: 서비스 예외로 변경 필요
        String encodeQuery;
        try {
            encodeQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패",e);
        }

        NaverNewsResponse response = naverNewsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", encodeQuery)
                        .queryParam("display", display)   // 최대 100
                        .queryParam("sort", sort)       // 최신순
                        .build())
                .retrieve()
                .body(NaverNewsResponse.class);

        return response != null ? response.getItems() : List.of();
    }



}
