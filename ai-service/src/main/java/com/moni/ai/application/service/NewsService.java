package com.moni.ai.application.service;

import com.moni.ai.common.exception.AiErrorCode;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import com.moni.ai.presentation.dto.response.NewsCreateResDto;
import com.moni.ai.presentation.dto.response.WatchCompanyResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final AsyncNewsCollectService asyncNewsCollectService;
    private final NewsCollectService newsCollectService;
    private final MarketNewsCollectService marketNewsCollectService;
    private final NewsRepository newsRepository;

    private static final Map<String, String> WATCH_LIST = WatchCompany.toMap();


    @Scheduled(cron = "0 0 8,18 * * MON-FRI") // 평일 오전 8시, 오후 6시
    public void collectAll() {
        log.info("뉴스 수집 스케줄러 시작");

        List<CompletableFuture<Void>> futures = WATCH_LIST.entrySet().stream()
                .map(entry -> asyncNewsCollectService.collectByTickerAsync(
                        entry.getKey(), entry.getValue()))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("뉴스 수집 스케줄러 완료");
    }

    @Scheduled(cron = "0 0 9,19 * * MON-FRI")
    public void collectMarketAll(){
        log.info("마켓 뉴스 스케줄러 시작");
        marketNewsCollectService.collectMarketNews();
        log.info("마켓 뉴스 스케줄러 완료");
    }

    public List<WatchCompanyResDto> getWatchList() {
        return Arrays.stream(WatchCompany.values())
                .map(WatchCompanyResDto::from)
                .toList();
    }

    @Transactional
    public NewsCreateResDto createNews(NewsCreateReqDto request){
        log.info("new생성 시작");
        //1. DB저장
        NewsEntity saved = newsCollectService.createNews(request);
        log.info("news저장 완료");
        //  2. 벡터 저장
        List<Document> documents = newsCollectService.toDocuments(saved);
        newsCollectService.addDocumentsVectorStore(documents);
        log.info("벡터 저장 완료");

        // 5. 반환
        return NewsCreateResDto.from(saved);
    }
}
