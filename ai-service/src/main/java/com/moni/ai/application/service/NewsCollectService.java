package com.moni.ai.application.service;

import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.enums.ImpactKeyword;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.response.NaverNewsResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectService {

    private final NaverNewsClient naverNewsClient;
    private final NewsRepository newsRepository;
    private final NewsFilterService newsFilterService;
    private final VectorStore vectorStore;
    // 주가 영향 키워드 & 기업 리스트
    private static final List<String> IMPACT_KEYWORDS = ImpactKeyword.getAllKeywords();
    private static final Map<String, String> WATCH_LIST = WatchCompany.toMap();


    @Scheduled(cron = "0 0 8,18 * * MON-FRI") // 평일 오전 8시, 오후 6시
    public void collectAll() {
        log.info("뉴스 수집 스케줄러 시작");
        WATCH_LIST.forEach(this::collectByTicker);
        log.info("뉴스 수집 스케줄러 완료");
    }

    @Transactional
    public void collectByTicker(String ticker, String companyName) {
        List<String> failedKeywords = new ArrayList<>();

        IMPACT_KEYWORDS.forEach(keyword -> {
            String query = companyName + " " + keyword;

            try {
                List<NaverNewsResDto.NaverNewsItem> items =
                        naverNewsClient.fetchNews(query, 10,"sim");

                List<NaverNewsResDto.NaverNewsItem> filtered= items.stream()
                        .filter(item -> !newsRepository.existsByUrl(item.getLink()))
                        .filter(newsFilterService::isWithinDays) // 3일 이내 쓰여진 기사
                        .filter(item -> newsFilterService.isRelevant(item, companyName))              //  기업명 위치
                        .filter(item -> newsFilterService.isKeywordNearCompany(                       //근접도
                                item.getCleanDescription(), companyName, keyword))
                        .toList();
                filtered.stream()
                        .map(item -> toEntity(item, ticker, companyName))
                        .forEach(newsRepository::save);

                List<Document> documents = filtered.stream()
                                .map(item -> toDocument(item,ticker,companyName,keyword))
                                        .toList();
                if (!documents.isEmpty()){
                    vectorStore.add(documents);
                }

                log.info("[{}] {} 키워드 완료 - {}건 저장", companyName, keyword, filtered.size());

            } catch (Exception e) {
                log.error("[{}] {} 키워드 수집 실패: {}", companyName, keyword, e.getMessage());
                failedKeywords.add(query);
            }
        });
        if (!failedKeywords.isEmpty()) {
            log.warn("[{}] 실패한 키워드 목록: {}", companyName, failedKeywords);
        }
    }

    private NewsEntity toEntity(NaverNewsResDto.NaverNewsItem item, String ticker,String company) {
        return NewsEntity.builder()
                .ticker(ticker)
                .title(item.getCleanTitle())
                .companyName(company)
                .content(item.getCleanDescription())
                .source(extractSource(item.getOriginallink()))
                .url(item.getLink())
                .publishedAt(item.getParsedPubDate())
                .build();
    }

    private Document toDocument(NaverNewsResDto.NaverNewsItem item,
                                String ticker, String company, String keyword){
        String content = "제목: " + item.getCleanTitle() + "\n내용: " + item.getCleanDescription();
        return new Document(
                content,
                Map.of(
                        "ticker",       ticker,
                        "company",      company,
                        "source",       extractSource(item.getOriginallink()),
                        "published_at", item.getParsedPubDate().toLocalDate().toString(),
                        "keyword",      keyword,
                        "url",          item.getLink()
                )
        );
    }

    // URL에서 언론사 추출 (예: news.naver.com → naver)
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
