package com.moni.ai.application.service;

import com.moni.ai.common.exception.AiErrorCode;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.enums.ImpactKeyword;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import com.moni.ai.presentation.dto.response.NaverNewsResDto;
import com.moni.ai.presentation.dto.response.NewsCreateResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final Integer CHUNKING_LENGTH = 2000;



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

                addDocumentsVectorStore(documents);

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

    //뉴스 저장
    @Transactional
    public NewsEntity createNews(NewsCreateReqDto request) {

        // ticker 검증
        WatchCompany company = WatchCompany.fromTicker(request.getTicker());

        //회사 검증
        if (!company.getCompanyName().equals(request.getCompanyName())) {
            throw new CustomException(AiErrorCode.COMPANY_NAME_MISMATCH);
        }

        //url  중복 체크
        if (newsRepository.existsByUrl(request.getUrl())) {
            throw new CustomException(AiErrorCode.NEWS_ALREADY_EXISTS);
        }

        //db
        NewsEntity newsEntity = NewsEntity.builder()
                .ticker(request.getTicker())
                .title(request.getTitle())
                .companyName(request.getCompanyName())
                .content(request.getContent())
                .source(request.getSource())
                .url(request.getUrl())
                .publishedAt(request.getPublishedAt())
                .build();

        return newsRepository.save(newsEntity);
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
    public List<Document> toDocuments(NewsEntity newsEntity){
        String fullContent = "제목: " + newsEntity.getTitle() + "\n내용: " + newsEntity.getContent();

        Map<String,Object> metadata=
                Map.of(
                        "ticker",       newsEntity.getTicker(),
                        "company",      newsEntity.getCompanyName(),
                        "source",       newsEntity.getSource(),
                        "published_at", newsEntity.getPublishedAt(),
                        "url",          newsEntity.getUrl()
                );

        if (fullContent.length()<CHUNKING_LENGTH){
            return List.of(new Document(fullContent, metadata));
        }

        List<Document> documents = new ArrayList<>();

        int start = 0;
        int chunkIndex = 0;

        while (start<fullContent.length()){
            int end = Math.min(start + CHUNKING_LENGTH, fullContent.length());
            String chunk = fullContent.substring(start,end);

            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunk_index", String.valueOf(chunkIndex));

            documents.add(new Document(chunk, chunkMetadata));
            start=end;
            chunkIndex++;
        }
        return documents;

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

    public void addDocumentsVectorStore(List<Document> documents){
        if (!documents.isEmpty()){
            vectorStore.add(documents);
        }
    }


}
