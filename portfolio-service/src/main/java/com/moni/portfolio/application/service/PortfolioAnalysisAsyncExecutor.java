package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.analysis.PortfolioRiskCalculator;
import com.moni.portfolio.application.analysis.TendencySuitabilityResult;
import com.moni.portfolio.application.analysis.TendencyType;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.global.config.AsyncConfig;
import com.moni.portfolio.infrastructure.client.AiServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.UserServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioHoldingRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioTendencyAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetAnalysisSnapshotResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioAnalysisAsyncExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String DEFAULT_USER_ROLE = "USER";
    private static final BigDecimal CONCENTRATION_THRESHOLD = new BigDecimal("60.00");

    private final AiServiceClient aiServiceClient;
    private final TradeServiceClient tradeServiceClient;
    private final UserServiceClient userServiceClient;
    private final PortfolioRiskCalculator portfolioRiskCalculator;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Async(AsyncConfig.PORTFOLIO_ANALYSIS_TASK_EXECUTOR)
    @Transactional
    public void requestAiAnalysis(UUID analysisId, UUID userId) {
        PortfolioAnalysis analysis = portfolioAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_NOT_FOUND));

        try {
            PortfolioAnalysisSnapshot snapshot = createSnapshot(userId);
            AiPortfolioAnalysisRequestDto request = snapshot.toAiRequest(analysisId, userId);
            boolean alreadySucceeded = analysis.getStatus() == AnalysisStatus.SUCCESS;
            ExternalApiResponseDto<AiPortfolioAnalysisResponseDto> response = aiServiceClient.analyzePortfolio(
                    userId,
                    DEFAULT_USER_ROLE,
                    request
            );
            AiPortfolioAnalysisResponseDto data = validateResponse(analysisId, response);
            analysis.succeed(
                    mergeSummary(data),
                    snapshot.totalReturnRate(),
                    snapshot.totalEvaluationAmount(),
                    request.concentrationScore(),
                    request.concentrationThreshold()
            );
            if (!alreadySucceeded) {
                analysis.getPortfolio().increaseAiAnalysisCount();
            }
        } catch (RetryableException exception) {
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_TIMEOUT.getMessage()));
        } catch (FeignException exception) {
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_ERROR.getMessage()));
        } catch (CustomException exception) {
            analysis.fail(truncate(exception.getMessage()));
        } catch (RuntimeException exception) {
            log.warn("포트폴리오 AI 분석 처리 실패. analysisId={}", analysisId, exception);
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_ERROR.getMessage()));
        }
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
            if (response == null || isInvalidUserTendency(response.data())) {
                log.warn("투자 성향 응답 데이터가 올바르지 않아 성향 분석을 생략합니다. userId={}", userId);
                return null;
            }
            TendencyType.fromName(response.data().type());
            return response.data();
        } catch (RetryableException exception) {
            log.warn("투자 성향 조회 시간이 초과되어 성향 분석을 생략합니다. userId={}", userId);
            return null;
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            log.warn(
                    "투자 성향 조회 중 User 서비스 오류가 발생해 성향 분석을 생략합니다. userId={}, status={}",
                    userId,
                    exception.status()
            );
            return null;
        } catch (IllegalArgumentException exception) {
            log.warn("지원하지 않는 투자 성향 타입이 내려와 성향 분석을 생략합니다. userId={}", userId);
            return null;
        }
    }

    private boolean isInvalidUserTendency(UserTendencyResponseDto userTendency) {
        return userTendency == null
                || userTendency.score() == null
                || userTendency.score() < 0
                || userTendency.score() > 100
                || userTendency.type() == null
                || userTendency.type().isBlank();
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

    private AiPortfolioAnalysisResponseDto validateResponse(
            UUID analysisId,
            ExternalApiResponseDto<AiPortfolioAnalysisResponseDto> response
    ) {
        if (response == null
                || response.data() == null
                || response.data().analysisId() == null
                || !analysisId.equals(response.data().analysisId())
                || response.data().summary() == null
                || response.data().summary().isBlank()
                || response.data().recommendation() == null
                || response.data().recommendation().isBlank()) {
            throw new CustomException(PortfolioErrorCode.AI_RESPONSE_INVALID);
        }
        if (response.data().tendencyAnalysis() != null
                && (response.data().tendencyAnalysis().summary() == null
                || response.data().tendencyAnalysis().summary().isBlank()
                || response.data().tendencyAnalysis().recommendation() == null
                || response.data().tendencyAnalysis().recommendation().isBlank())) {
            throw new CustomException(PortfolioErrorCode.AI_RESPONSE_INVALID);
        }

        return response.data();
    }

    private String mergeSummary(AiPortfolioAnalysisResponseDto data) {
        StringBuilder summary = new StringBuilder();
        summary.append(data.summary());
        if (data.tendencyAnalysis() != null) {
            summary.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("성향 적합도: ")
                    .append(data.tendencyAnalysis().summary())
                    .append(System.lineSeparator())
                    .append("성향 권고: ")
                    .append(data.tendencyAnalysis().recommendation());
        }
        summary.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("권고: ")
                .append(data.recommendation());

        return summary.toString();
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
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
