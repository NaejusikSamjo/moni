package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.portfolio.application.policy.PortfolioAnalysisPolicyService;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final ZoneId ANALYSIS_DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<AnalysisStatus> VISIBLE_ANALYSIS_STATUSES = List.of(
            AnalysisStatus.PENDING,
            AnalysisStatus.SUCCESS
    );

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final PortfolioAnalysisPolicyService portfolioAnalysisPolicyService;
    private final PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @Transactional
    public PortfolioAnalysisCreateResponseDto requestAnalysis(UUID userId) {
        Portfolio portfolio = findPortfolioForUpdate(userId);
        Optional<PortfolioAnalysis> pendingAnalysis = findPendingAnalysisToday(portfolio);
        if (pendingAnalysis.isPresent()) {
            return PortfolioAnalysisCreateResponseDto.from(pendingAnalysis.get());
        }

        portfolioAnalysisPolicyService.validateRequest(userId, portfolio);
        PortfolioAnalysis analysis = PortfolioAnalysis.request(portfolio);
        PortfolioAnalysis savedAnalysis = portfolioAnalysisRepository.save(analysis);

        requestAiAnalysisAfterCommit(savedAnalysis.getId(), userId);

        return PortfolioAnalysisCreateResponseDto.from(savedAnalysis);
    }

    private Optional<PortfolioAnalysis> findPendingAnalysisToday(Portfolio portfolio) {
        LocalDate today = LocalDate.now(ANALYSIS_DAILY_LIMIT_ZONE);
        LocalDateTime startDateTime = today.atStartOfDay();
        LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();
        return portfolioAnalysisRepository.findPendingByPortfolioIdAndCreatedAtBetween(
                portfolio.getId(),
                startDateTime,
                endDateTime
        );
    }

    public PortfolioAnalysisResponseDto getLatestAnalysis(UUID userId) {
        Portfolio portfolio = findPortfolio(userId);
        PortfolioAnalysis analysis = portfolioAnalysisRepository.findLatestSuccessByPortfolioId(portfolio.getId())
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_NOT_FOUND));

        return toResponse(analysis);
    }

    public PortfolioAnalysisResponseDto getAnalysis(UUID userId, UUID analysisId) {
        Portfolio portfolio = findPortfolio(userId);
        PortfolioAnalysis analysis = portfolioAnalysisRepository.findByIdAndPortfolioId(analysisId, portfolio.getId())
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_NOT_FOUND));

        return toResponse(analysis);
    }

    public PageRes<PortfolioAnalysisResponseDto> getAnalyses(UUID userId, int page, int size) {
        Portfolio portfolio = findPortfolio(userId);
        PageRequest pageRequest = PageRequest.of(resolvePage(page), resolveSize(size));
        Page<PortfolioAnalysisResponseDto> analyses = portfolioAnalysisRepository
                .findAllByPortfolioIdAndStatusIn(portfolio.getId(), VISIBLE_ANALYSIS_STATUSES, pageRequest)
                .map(this::toResponse);

        return new PageRes<>(analyses);
    }

    private void requestAiAnalysisAfterCommit(UUID analysisId, UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(analysisId, userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                portfolioAnalysisAsyncExecutor.requestAiAnalysis(analysisId, userId);
            }
        });
    }

    private PortfolioAnalysisResponseDto toResponse(PortfolioAnalysis analysis) {
        return PortfolioAnalysisResponseDto.from(analysis);
    }

    private Portfolio findPortfolio(UUID userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private Portfolio findPortfolioForUpdate(UUID userId) {
        return portfolioRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private int resolvePage(int page) {
        if (page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int resolveSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
