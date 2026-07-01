package com.moni.ai.application.service;

import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.presentation.dto.response.WatchCompanyResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<WatchCompanyResDto> getWatchList() {
        return Arrays.stream(WatchCompany.values())
                .map(WatchCompanyResDto::from)
                .toList();
    }
}
