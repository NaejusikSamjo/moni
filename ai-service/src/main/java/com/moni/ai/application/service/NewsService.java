package com.moni.ai.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import com.moni.ai.presentation.dto.request.NewsSearchReqDto;
import com.moni.ai.presentation.dto.response.NewsCreateResDto;
import com.moni.ai.presentation.dto.response.NewsPageCacheDto;
import com.moni.ai.presentation.dto.response.NewsResDto;
import com.moni.ai.presentation.dto.response.WatchCompanyResDto;
import com.moni.common.response.paging.PageRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j

public class NewsService {

    private final AsyncNewsCollectService asyncNewsCollectService;
    private final NewsCollectService newsCollectService;
    private final MarketNewsCollectService marketNewsCollectService;
    private final NewsRepository newsRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;


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

        //캐시 만료
        evictNewsCache();

        // 5. 반환
        return NewsCreateResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageRes<NewsResDto> getNewsList(NewsSearchReqDto request, Pageable pageable) {
        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        String cacheKey= buildCacheKey(request,targetDate,pageable);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if(cached!=null){
            log.debug("뉴스 캐시 히트 - key: {}", cacheKey);
            try {
                NewsPageCacheDto dto = objectMapper.readValue(cached, NewsPageCacheDto.class);
                log.debug("뉴스 캐시 히트 - key: {}", cacheKey);
                return dto.toPageRes(pageable);
            } catch (Exception e) {
                log.warn("뉴스 캐시 역직렬화 실패 - key: {}", cacheKey);
            }
        }

        LocalDate minDate = targetDate.minusDays(3); // 최대 3일 전까지
        LocalDate currentDate = targetDate;

        while (!currentDate.isBefore(minDate)) {
            // 해당 날짜로 조회
            request.setDate(currentDate);
            Page<NewsEntity> page = newsRepository.searchNews(request, pageable);

            if (!page.isEmpty()) {
                PageRes<NewsResDto> result = new PageRes<>(page.map(NewsResDto::from));

                try{
                    String json = objectMapper.writeValueAsString(NewsPageCacheDto.from(result));
                  redisTemplate.opsForValue().set(cacheKey,json,30, TimeUnit.MINUTES);
                  log.debug("뉴스 캐시 저장 - key:{}",cacheKey);
                }catch(Exception e){
                    log.warn("뉴스 캐시 저장 실패 - key{}",cacheKey);
                }
                return result;
            }

            // 데이터 없으면 하루 전으로
            currentDate = currentDate.minusDays(1);
        }

        // 3일 내 데이터 없으면 빈 페이지 반환
        return new PageRes<>(Page.empty(pageable));
    }

    // 캐시 키 생성
    private String buildCacheKey(NewsSearchReqDto request, LocalDate date, Pageable pageable) {
        return String.format("news:list:%s:%s:%s:%d:%d",
                request.getTicker() != null ? request.getTicker() : "all",
                request.getCompanyName() != null ? request.getCompanyName() : "all",
                request.getKeyword() != null ? request.getKeyword() : "all",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    // news:list:* 패턴으로 캐시 전체 만료
    private void evictNewsCache() {
        try {
            Set<String> keys = redisTemplate.keys("news:list:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("뉴스 캐시 만료 - {}건", keys.size());
            }
        } catch (Exception e) {
            log.warn("뉴스 캐시 만료 실패: {}", e.getMessage());
        }
    }
}
