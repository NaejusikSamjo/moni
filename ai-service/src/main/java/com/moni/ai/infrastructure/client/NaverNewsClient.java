package com.moni.ai.infrastructure.client;

import com.moni.ai.presentation.dto.response.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NaverNewsClient {

    private final RestClient naverNewsRestClient;


    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2) // 2초 → 4초 → 실패
    )
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

    // 빈 리스트 반환 → 해당 키워드 조합 스킵하고 다음 진행
    @Recover
    public List<NaverNewsResponse.NaverNewsItem> fetchNewsRecover(Exception e, String query, int display, String sort) {
        log.error("Naver API 최종 실패 - query: {}, 사유: {}", query, e.getMessage());
        return List.of();
    }


}
