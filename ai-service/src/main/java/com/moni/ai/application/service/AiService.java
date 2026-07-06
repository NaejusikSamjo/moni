package com.moni.ai.application.service;

import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.enums.MarketKeyword;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.enums.SentimentEnum;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.presentation.dto.response.AiNewsAnalysisResDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.ai.presentation.dto.response.MarketAnalysisResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AnalysisSaveService analysisSaveService;
    private final LlmAnalysisService llmAnalysisService;
    private final RedissonClient redissonClient;

    @Value("classpath:prompts/ai-system-prompt.st")
    private Resource companyPromptResource;

    @Value("classpath:prompts/market-system-prompt.st")
    private Resource marketPromptResource;

    //분석 결과 유효 시간
    private static final int CACHE_HOURS = 6;
    private static final long LOCK_WAIT_TIME=0L; //락 대기 없이 바로 실패
    private static final long LOCK_LEASE_TIME=30L; // 30초 후 자동 해제

    public CompanyIssueResDto companyAnalyze(String ticker) {

        WatchCompany company = WatchCompany.fromTicker(ticker);

        String lockKey= "lock:analysis:company:" + ticker;
        RLock lock = redissonClient.getLock(lockKey);

        try {

            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("[{}] 분석 진행 중 - 락 획득 실패", ticker);
                throw new CustomException(AiErrorCode.ANALYSIS_IN_PROGRESS);
            }
            // 1. 유효한 캐시 조회
            Optional<CompanyIssueAnalysisEntity> cached = analysisSaveService.getCachedAnalysis(ticker);

            if (cached.isPresent()) {
                log.info("[{}] 분석 존재", ticker);
                throw new CustomException(AiErrorCode.ANALYSIS_ALREADY_EXISTS);
            }
            BeanOutputConverter<AiNewsAnalysisResDto> parser = new BeanOutputConverter<>(AiNewsAnalysisResDto.class);

            // 프롬프트 로드 및 변수 치환
            String systemPrompt = loadPrompt(companyPromptResource)
                    .replace("{ticker}", ticker)
                    .replace("{companyName}", company.getCompanyName())
                    .replace("{format}", parser.getFormat());

            String query = "[" + ticker + " " + company.getCompanyName() + "] " + company.getCompanyName() + " 기업의 최근 주요 이슈와 뉴스만 분석해줘.";



            String filterExpression = getFilterExpression(ticker);


            AiNewsAnalysisResDto result = llmAnalysisService.createLlmAnalysis(systemPrompt,query,filterExpression);
            // 5. sentiment 추출 (응답에서 POSITIVE/NEGATIVE/NEUTRAL 파싱)
            SentimentEnum sentiment = SentimentEnum.valueOf(result.getSentiment());
            // 6. 분석 결과 저장
            CompanyIssueAnalysisEntity entity = CompanyIssueAnalysisEntity.builder()
                    .ticker(ticker)
                    .companyName(company.getCompanyName())
                    .summary(result.getSummary())
                    .sentiment(sentiment)
                    .expiredAt(LocalDateTime.now().plusHours(CACHE_HOURS))
                    .build();

            CompanyIssueAnalysisEntity saved = analysisSaveService.save(entity);

            analysisSaveService.saveLog(combinePrompt(systemPrompt,query,filterExpression),saved,null);

            log.info("[{}] 분석 결과 저장 완료 - sentiment: {}", ticker, sentiment);

            return CompanyIssueResDto.toDto(saved);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        } finally {
            // 현재 스레드가 락을 쥐고 있는지 자체 검증 후 안전하게 해제
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public MarketAnalysisResDto analyzeMarket(String keyword) {


        MarketKeyword marketKeyword = MarketKeyword.fromKeyword(keyword);

        String lockKey = "lock:analysis:market:" + keyword;
        RLock lock = redissonClient.getLock(lockKey);

        try{

            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME,TimeUnit.SECONDS);
            if (!isLocked){
                log.warn("[{}] 분석 진행 중 - 락 획득 실패", keyword);
                throw new CustomException(AiErrorCode.ANALYSIS_IN_PROGRESS);
            }
            // 유효한 캐시 조회
            Optional<MarketNewsAnalysisEntity> cached = analysisSaveService.getCachedMarketAnalysis(keyword);
            if (cached.isPresent()) {
                log.info("[{}] 마켓 분석 존재", keyword);
                throw new CustomException(AiErrorCode.ANALYSIS_ALREADY_EXISTS);
            }

            BeanOutputConverter<AiNewsAnalysisResDto> parser = new BeanOutputConverter<>(AiNewsAnalysisResDto.class);

            String systemPrompt = loadPrompt(marketPromptResource)
                    .replace("{keyword}", marketKeyword.getKeyword())
                    .replace("{format}", parser.getFormat());

            String query = marketKeyword.getKeyword() + " 관련 최근 시장 이슈 분석해줘.";

            String filterExpression = getMarketFilterExpression(marketKeyword.getKeyword());

            AiNewsAnalysisResDto llmResult = llmAnalysisService.createLlmAnalysis(systemPrompt, query, filterExpression);

            MarketNewsAnalysisEntity entity = MarketNewsAnalysisEntity.builder()
                    .summary(llmResult.getSummary())
                    .expiredAt(LocalDateTime.now().plusHours(CACHE_HOURS))
                    .build();

            MarketNewsAnalysisEntity saved = analysisSaveService.save(entity);
            analysisSaveService.saveLog(combinePrompt(systemPrompt,query,filterExpression),null,saved);

            log.info("[{}] 분석 결과 저장 완료", keyword);
            return MarketAnalysisResDto.from(saved);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        }finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




    public CompanyIssueResDto getLatestAnalysis(String ticker) {

        WatchCompany.fromTicker(ticker);

        return analysisSaveService.getLatestAnalysis(ticker)
                .map(CompanyIssueResDto::toDto)
                .orElseThrow(()-> new CustomException(AiErrorCode.AI_NOT_FOUND));
    }



    private String loadPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        }
    }

    private String combinePrompt(String systemPrompt,String query, String filterExpression){
        String prompt = "=== SYSTEM ===\n" + systemPrompt +
                "\n=== QUERY ===\n" + query +
                "\n=== FILTER ===\n" + filterExpression;

        return prompt;
    }

    private String getFilterExpression(String ticker) {
        int searchDays = switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY,SUNDAY -> 3;   // 금~월 커버
            case TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> 1;
            case SATURDAY -> 2;  // 금~토 커버
        };

        String fromDate = LocalDate.now().minusDays(searchDays).toString();
        return "ticker == '" + ticker + "' && published_at >= '" + fromDate + "'";
    }

    private String getMarketFilterExpression(String keyword) {
        int searchDays = switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY,SUNDAY -> 3;   // 금~월 커버
            case TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> 1;
            case SATURDAY -> 2;  // 금~토 커버
        };

        String fromDate = LocalDate.now().minusDays(searchDays).toString();
        return "category == 'MARKET' && keyword == '" + keyword + "' && published_at >= '" + fromDate + "'";
    }

}
