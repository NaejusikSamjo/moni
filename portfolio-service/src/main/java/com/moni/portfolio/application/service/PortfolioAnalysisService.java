package com.moni.portfolio.application.service;

import com.moni.common.response.paging.PageRes;
import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.analysis.PortfolioRiskCalculator;
import com.moni.portfolio.application.analysis.TendencyType;
import com.moni.portfolio.application.analysis.TendencySuitabilityResult;
import com.moni.portfolio.application.policy.PortfolioAnalysisPolicyService;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.UserServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioHoldingRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioTendencyAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetAnalysisSnapshotResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisResponseDto;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final BigDecimal CONCENTRATION_THRESHOLD = new BigDecimal("60.00");
    private static final ZoneId ANALYSIS_DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<AnalysisStatus> VISIBLE_ANALYSIS_STATUSES = List.of(
            AnalysisStatus.PENDING,
            AnalysisStatus.SUCCESS
    );

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final PortfolioAnalysisPolicyService portfolioAnalysisPolicyService;
    private final PortfolioRiskCalculator portfolioRiskCalculator;
    private final TradeServiceClient tradeServiceClient;
    private final UserServiceClient userServiceClient;
    private final PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @Transactional
    public PortfolioAnalysisCreateResponseDto requestAnalysis(UUID userId) {
        Portfolio portfolio = findPortfolioForUpdate(userId);
        Optional<PortfolioAnalysis> pendingAnalysis = findPendingAnalysisToday(portfolio);
        if (pendingAnalysis.isPresent()) {
            return PortfolioAnalysisCreateResponseDto.from(pendingAnalysis.get());
        }

        portfolioAnalysisPolicyService.validateRequest(userId, portfolio);
        PortfolioAnalysisSnapshot snapshot = createSnapshot(userId);

        PortfolioAnalysis analysis = PortfolioAnalysis.request(
                portfolio,
                snapshot.totalReturnRate(),
                snapshot.totalEvaluationAmount()
        );
        PortfolioAnalysis savedAnalysis = portfolioAnalysisRepository.save(analysis);

        requestAiAnalysisAfterCommit(
                savedAnalysis.getId(),
                snapshot.toAiRequest(savedAnalysis.getId(), userId)
        );

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

    private PortfolioAnalysisSnapshot createSnapshot(UUID userId) {
        TradeAssetAnalysisSnapshotResponseDto snapshot = getAnalysisSnapshot(userId);
        if (snapshot.holdings().isEmpty()) {
            throw new CustomException(PortfolioErrorCode.INVALID_PORTFOLIO_QUERY);
        }

        List<PortfolioHoldingSnapshot> holdingSnapshots = snapshot.holdings().stream()
                .map(this::toHoldingSnapshot)
                .toList();
        BigDecimal concentrationScore = calculateHoldingConcentrationScore(holdingSnapshots);
        AiPortfolioTendencyAnalysisRequestDto tendencyAnalysis =
                createTendencyAnalysis(userId, snapshot, concentrationScore, holdingSnapshots);

        return new PortfolioAnalysisSnapshot(
                snapshot.stockEvaluationAmount(),
                snapshot.totalReturnRate(),
                concentrationScore,
                CONCENTRATION_THRESHOLD,
                holdingSnapshots,
                tendencyAnalysis
        );
    }

    private AiPortfolioTendencyAnalysisRequestDto createTendencyAnalysis(
            UUID userId,
            TradeAssetAnalysisSnapshotResponseDto snapshot,
            BigDecimal concentrationScore,
            List<PortfolioHoldingSnapshot> holdingSnapshots
    ) {
        UserTendencyResponseDto userTendency = getUserTendency(userId);
        if (userTendency == null) {
            return null;
        }

        TendencySuitabilityResult result = portfolioRiskCalculator.calculate(
                userTendency,
                snapshot.totalAsset(),
                snapshot.stockEvaluationAmount(),
                concentrationScore,
                holdingSnapshots.size()
        );
        return result.toAiRequest();
    }

    private UserTendencyResponseDto getUserTendency(UUID userId) {
        try {
            ExternalApiResponseDto<UserTendencyResponseDto> response = userServiceClient.getTendency(userId);
            if (response == null || response.data() == null) {
                throw new CustomException(PortfolioErrorCode.USER_TENDENCY_RESPONSE_INVALID);
            }
            if (response.data().score() == null
                    || response.data().score() < 0
                    || response.data().score() > 100
                    || response.data().type() == null
                    || response.data().type().isBlank()) {
                throw new CustomException(PortfolioErrorCode.USER_TENDENCY_RESPONSE_INVALID);
            }
            TendencyType.fromName(response.data().type());
            return response.data();
        } catch (RetryableException exception) {
            throw new CustomException(PortfolioErrorCode.USER_SERVICE_TIMEOUT);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw new CustomException(PortfolioErrorCode.USER_SERVICE_ERROR);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(PortfolioErrorCode.USER_TENDENCY_RESPONSE_INVALID);
        }
    }

    private PortfolioHoldingSnapshot toHoldingSnapshot(TradeAssetHoldingResponseDto holding) {
        if (holding.stockName() == null || holding.stockName().isBlank()) {
            throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
        }

        return new PortfolioHoldingSnapshot(
                holding.ticker(),
                holding.stockName(),
                holding.quantity(),
                holding.averagePurchasePrice(),
                holding.currentPrice(),
                holding.evaluationAmount(),
                holding.profitLoss(),
                holding.profitRate(),
                holding.weight()
        );
    }

    private BigDecimal calculateHoldingConcentrationScore(List<PortfolioHoldingSnapshot> holdings) {
        return holdings.stream()
                .map(PortfolioHoldingSnapshot::weight)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private void requestAiAnalysisAfterCommit(UUID analysisId, AiPortfolioAnalysisRequestDto request) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(analysisId, request);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                portfolioAnalysisAsyncExecutor.requestAiAnalysis(analysisId, request);
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

    private TradeAssetAnalysisSnapshotResponseDto getAnalysisSnapshot(UUID userId) {
        TradeAssetAnalysisSnapshotResponseDto snapshot =
                requestTradeData(() -> tradeServiceClient.getAnalysisSnapshot(userId));
        if (snapshot.totalAsset() == null
                || snapshot.stockEvaluationAmount() == null
                || snapshot.totalReturnRate() == null
                || snapshot.holdings() == null) {
            throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
        }

        snapshot.holdings().forEach(this::validateAssetHolding);
        return snapshot;
    }

    private void validateAssetHolding(TradeAssetHoldingResponseDto holding) {
        if (holding == null
                || holding.ticker() == null
                || holding.ticker().isBlank()
                || holding.stockName() == null
                || holding.stockName().isBlank()
                || holding.quantity() == null
                || holding.averagePurchasePrice() == null
                || holding.currentPrice() == null
                || holding.evaluationAmount() == null
                || holding.profitLoss() == null
                || holding.profitRate() == null
                || holding.weight() == null) {
            throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
        }
    }

    private <T> T requestTradeData(Supplier<ExternalApiResponseDto<T>> request) {
        try {
            ExternalApiResponseDto<T> response = request.get();
            if (response == null || response.data() == null) {
                throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
            }
            return response.data();
        } catch (RetryableException exception) {
            throw new CustomException(PortfolioErrorCode.TRADE_SERVICE_TIMEOUT);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                throw new CustomException(PortfolioErrorCode.TRADE_ACCOUNT_NOT_FOUND);
            }
            throw new CustomException(PortfolioErrorCode.TRADE_SERVICE_ERROR);
        }
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

    private record PortfolioAnalysisSnapshot(
            BigDecimal totalEvaluationAmount,
            BigDecimal totalReturnRate,
            BigDecimal concentrationScore,
            BigDecimal concentrationThreshold,
            List<PortfolioHoldingSnapshot> holdings,
            AiPortfolioTendencyAnalysisRequestDto tendencyAnalysis
    ) {

        private AiPortfolioAnalysisRequestDto toAiRequest(UUID analysisId, UUID userId) {
            return new AiPortfolioAnalysisRequestDto(
                    analysisId,
                    userId,
                    totalEvaluationAmount,
                    totalReturnRate,
                    concentrationScore,
                    concentrationThreshold,
                    holdings.stream()
                            .map(PortfolioHoldingSnapshot::toAiRequest)
                            .toList(),
                    tendencyAnalysis
            );
        }
    }

    private record PortfolioHoldingSnapshot(
            String ticker,
            String stockName,
            Long quantity,
            BigDecimal averagePurchasePrice,
            BigDecimal currentPrice,
            BigDecimal evaluationAmount,
            BigDecimal profitLoss,
            BigDecimal profitRate,
            BigDecimal weight
    ) {

        private AiPortfolioHoldingRequestDto toAiRequest() {
            return new AiPortfolioHoldingRequestDto(
                    ticker,
                    stockName,
                    quantity,
                    averagePurchasePrice,
                    currentPrice,
                    evaluationAmount,
                    profitLoss,
                    profitRate,
                    weight
            );
        }
    }
}
