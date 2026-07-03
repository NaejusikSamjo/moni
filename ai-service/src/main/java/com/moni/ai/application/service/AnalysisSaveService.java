package com.moni.ai.application.service;

import com.moni.ai.domain.entity.AiLogEntity;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.repository.AiLogRepository;
import com.moni.ai.domain.repository.CompanyIssueAnalysisRepository;
import com.moni.ai.domain.repository.MarketNewsAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalysisSaveService {

    private final CompanyIssueAnalysisRepository companyIssueAnalysisRepository;
    private final MarketNewsAnalysisRepository marketNewsAnalysisRepository;
    private final AiLogRepository aiLogRepository;

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
