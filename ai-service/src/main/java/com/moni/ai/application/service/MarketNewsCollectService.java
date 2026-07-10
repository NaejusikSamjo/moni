package com.moni.ai.application.service;

import com.moni.ai.domain.entity.MarketNewsEntity;
import com.moni.ai.domain.enums.MarketKeyword;
import com.moni.ai.domain.repository.MarketNewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.response.NaverNewsResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketNewsCollectService {

    private final NaverNewsClient naverNewsClient;
    private final MarketNewsRepository marketNewsRepository;
    private final NewsFilterService newsFilterService;
    private final VectorStore vectorStore;

    private static final List<String> MARKET_KEYWORDS = MarketKeyword.getAllKeywords();

    public void collectMarketNews() {
        MARKET_KEYWORDS.forEach(keyword -> {
            try {
                List<NaverNewsResDto.NaverNewsItem> items =
                        naverNewsClient.fetchNews(keyword, 10, "sim");

                List<NaverNewsResDto.NaverNewsItem> filtered = items.stream()
                        .filter(item -> !marketNewsRepository.existsByUrl(item.getLink()))
                        .filter(newsFilterService::isWithinDays)
                        .toList();

                filtered.stream()
                        .map(item -> toEntity(item, keyword))
                        .forEach(marketNewsRepository::save);

                // 벡터 저장
                List<Document> documents = filtered.stream()
                        .map(item -> toDocument(item, keyword))
                        .toList();
                if (!documents.isEmpty()) {
                    vectorStore.add(documents);
                }

                log.info("[시장] {} 키워드 완료 - {}건 저장", keyword, filtered.size());

            } catch (Exception e) {
                log.error("[시장] {} 키워드 수집 실패: {}", keyword, e.getMessage());
            }
        });
    }

    private MarketNewsEntity toEntity(NaverNewsResDto.NaverNewsItem item, String keyword) {
        return MarketNewsEntity.builder()
                .title(item.getCleanTitle())
                .content(item.getCleanDescription())
                .source(extractSource(item.getOriginallink()))
                .url(item.getLink())
                .publishedAt(item.getParsedPubDate())
                .keyword(keyword)
                .build();
    }

    private Document toDocument(NaverNewsResDto.NaverNewsItem item, String keyword) {
        String content = "제목: " + item.getCleanTitle() + "\n내용: " + item.getCleanDescription();
        return new Document(
                content,
                Map.of(
                        "category", "MARKET",
                        "keyword",  keyword,
                        "source",   extractSource(item.getOriginallink()),
                        "published_at", item.getParsedPubDate().toLocalDate().toString(),
                        "url",      item.getLink()
                )
        );
    }

    private String extractSource(String url) {
        try {
            return URI.create(url).getHost()
                    .replace("www.", "")
                    .split("\\.")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }
}
