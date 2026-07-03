package com.moni.ai.application.service;

import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.entity.MarketNewsEntity;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.domain.repository.CompanyIssueAnalysisRepository;
import com.moni.ai.domain.repository.MarketNewsAnalysisRepository;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.parser.Entity;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalysisSaveService {

    private final CompanyIssueAnalysisRepository companyIssueAnalysisRepository;
    private final MarketNewsAnalysisRepository marketNewsAnalysisRepository;

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

}
