package com.moni.ai.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.ai.domain.entity.AiLogEntity;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.repository.AiLogRepository;
import com.moni.ai.domain.repository.CompanyIssueAnalysisRepository;
import com.moni.ai.domain.repository.MarketNewsAnalysisRepository;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AnalysisSaveService {

    private final CompanyIssueAnalysisRepository companyIssueAnalysisRepository;
    private final MarketNewsAnalysisRepository marketNewsAnalysisRepository;
    private final AiLogRepository aiLogRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long ANALYSIS_CACHE_TTL = 3L;

    // 분석 결과 Redis 저장
    public void cacheAnalysis(String ticker, CompanyIssueResDto dto) {
        try {
            String key = "analysis:company:" + ticker;
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(dto),
                    ANALYSIS_CACHE_TTL, TimeUnit.HOURS
            );
            log.debug("[{}] 분석 Redis 캐시 저장", ticker);
        } catch (Exception e) {
            log.warn("[{}] 분석 Redis 캐시 저장 실패: {}", ticker, e.getMessage());
        }
    }

    public Optional<CompanyIssueResDto> getCachedAnalysisFromRedis(String ticker) {
        try {
            String key = "analysis:company:" + ticker;
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                CompanyIssueResDto dto = objectMapper.readValue(cached, CompanyIssueResDto.class);

                // expiredAt 지났으면 캐시 미스 처리
                if (dto.getExpiredAt() != null && dto.getExpiredAt().isBefore(LocalDateTime.now())) {
                    log.debug("[{}] 분석 캐시 만료 - expiredAt: {}", ticker, dto.getExpiredAt());
                    redisTemplate.delete(key);
                    return Optional.empty();
                }

                log.debug("[{}] 분석 Redis 캐시 히트", ticker);
                return Optional.of(dto);
            }
        } catch (Exception e) {
            log.warn("[{}] 분석 Redis 캐시 조회 실패: {}", ticker, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<CompanyIssueAnalysisEntity>  getCachedAnalysis(String ticker){
        return companyIssueAnalysisRepository.findLatestValidAnalysis(ticker);
    }

    @Transactional
    public CompanyIssueAnalysisEntity save(CompanyIssueAnalysisEntity entity){
        return companyIssueAnalysisRepository.save(entity);
    }


    public Optional<CompanyIssueAnalysisEntity> getLatestAnalysis(String ticker){
        return companyIssueAnalysisRepository.findLatestValidAnalysis(ticker);
    }

    @Transactional
    public MarketNewsAnalysisEntity save(MarketNewsAnalysisEntity entity){
        return marketNewsAnalysisRepository.save(entity);
    }

    public Optional<MarketNewsAnalysisEntity> getCachedMarketAnalysis(String keyword) {
        return marketNewsAnalysisRepository
                .findTopByKeywordAndExpiredAtAfterOrderByCreatedAtDesc(keyword, LocalDateTime.now());
    }

    @Transactional
    public AiLogEntity saveLog(
            String prompt,
            CompanyIssueAnalysisEntity companyIssueAnalysis,
            MarketNewsAnalysisEntity marketNewsAnalysis
    ){

        return aiLogRepository.save(
                AiLogEntity.builder()
                        .prompt(prompt)
                        .companyAnalysis(companyIssueAnalysis)
                        .marketAnalysis(marketNewsAnalysis)
                        .build()
        );
    }

}
