package com.moni.ai.application.service;


import com.moni.ai.domain.enums.WatchCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncNewsCollectService {

    private final NewsCollectService newsCollectService;

    @Async
    public CompletableFuture<Void> collectByTickerAsync(String ticker, String companyName) {
        log.debug("[{}] {} 수집 시작 - 스레드: {}", ticker, companyName,
                Thread.currentThread().getName());
        newsCollectService.collectByTicker(ticker, companyName);
        return CompletableFuture.completedFuture(null);
    }


}
