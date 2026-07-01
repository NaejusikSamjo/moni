package com.moni.portfolio.application.service;

import com.moni.common.response.paging.PageRes;
import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.analysis.PortfolioRiskCalculator;
import com.moni.portfolio.application.analysis.TendencyType;
import com.moni.portfolio.application.analysis.TendencySuitabilityResult;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.domain.repository.PortfolioSectorAnalysisRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.UserServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioHoldingRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioSectorAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioTendencyAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.StockResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingsResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetResponseDto;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CONCENTRATION_THRESHOLD = new BigDecimal("60.00");
    private static final String DEFAULT_SECTOR_NAME = "미분류";
    private static final String DEFAULT_ASSET_HOLDINGS_SORT = "evaluationAmount,desc";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final PortfolioSectorAnalysisRepository portfolioSectorAnalysisRepository;
    private final PortfolioRiskCalculator portfolioRiskCalculator;
    private final TradeServiceClient tradeServiceClient;
    private final StockServiceClient stockServiceClient;
    private final UserServiceClient userServiceClient;
    private final PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @Transactional
    public PortfolioAnalysisCreateResponseDto requestAnalysis(UUID userId) {
        Portfolio portfolio = findPortfolio(userId);
        PortfolioAnalysisSnapshot snapshot = createSnapshot(userId);

        PortfolioAnalysis analysis = PortfolioAnalysis.request(
                portfolio,
                snapshot.totalReturnRate(),
                snapshot.totalEvaluationAmount()
        );
        PortfolioAnalysis savedAnalysis = portfolioAnalysisRepository.save(analysis);
        portfolio.increaseAiAnalysisCount();
        saveSectorAnalyses(savedAnalysis, snapshot.sectorAnalyses());

        requestAiAnalysisAfterCommit(
                savedAnalysis.getId(),
                snapshot.toAiRequest(savedAnalysis.getId(), userId)
        );

        return PortfolioAnalysisCreateResponseDto.from(savedAnalysis);
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
                .findAllByPortfolioId(portfolio.getId(), pageRequest)
                .map(this::toResponse);

        return new PageRes<>(analyses);
    }

    private PortfolioAnalysisSnapshot createSnapshot(UUID userId) {
        TradeAssetResponseDto asset = getAssets(userId);
        List<TradeAssetHoldingResponseDto> holdings = getAssetHoldings(userId);
        if (holdings.isEmpty()) {
            throw new CustomException(PortfolioErrorCode.INVALID_PORTFOLIO_QUERY);
        }

        Map<String, StockResponseDto> stocks = getStocks(holdings);
        List<PortfolioHoldingSnapshot> holdingSnapshots = holdings.stream()
                .map(holding -> toHoldingSnapshot(holding, stocks.get(holding.ticker())))
                .toList();
        List<PortfolioSectorSnapshot> sectorSnapshots = createSectorSnapshots(holdingSnapshots);
        BigDecimal concentrationScore = sectorSnapshots.stream()
                .map(PortfolioSectorSnapshot::weight)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        AiPortfolioTendencyAnalysisRequestDto tendencyAnalysis =
                createTendencyAnalysis(userId, asset, concentrationScore, holdingSnapshots);

        return new PortfolioAnalysisSnapshot(
                asset.stockEvaluationAmount(),
                asset.totalReturnRate(),
                concentrationScore,
                CONCENTRATION_THRESHOLD,
                sectorSnapshots,
                holdingSnapshots,
                tendencyAnalysis
        );
    }

    private AiPortfolioTendencyAnalysisRequestDto createTendencyAnalysis(
            UUID userId,
            TradeAssetResponseDto asset,
            BigDecimal concentrationScore,
            List<PortfolioHoldingSnapshot> holdingSnapshots
    ) {
        UserTendencyResponseDto userTendency = getUserTendency(userId);
        if (userTendency == null) {
            return null;
        }

        BigDecimal topHoldingWeight = holdingSnapshots.stream()
                .map(PortfolioHoldingSnapshot::weight)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        TendencySuitabilityResult result = portfolioRiskCalculator.calculate(
                userTendency,
                asset.totalAsset(),
                asset.stockEvaluationAmount(),
                topHoldingWeight,
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

    private PortfolioHoldingSnapshot toHoldingSnapshot(TradeAssetHoldingResponseDto holding, StockResponseDto stock) {
        if (stock == null || stock.name() == null || stock.name().isBlank()) {
            throw new CustomException(PortfolioErrorCode.STOCK_RESPONSE_INVALID);
        }

        return new PortfolioHoldingSnapshot(
                holding.ticker(),
                stock.name(),
                resolveSectorName(stock.sectorName()),
                holding.quantity(),
                holding.averagePurchasePrice(),
                holding.currentPrice(),
                holding.evaluationAmount(),
                holding.profitLoss(),
                holding.profitRate(),
                holding.weight()
        );
    }

    private List<PortfolioSectorSnapshot> createSectorSnapshots(List<PortfolioHoldingSnapshot> holdings) {
        BigDecimal totalEvaluationAmount = holdings.stream()
                .map(PortfolioHoldingSnapshot::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> evaluationAmountBySector = holdings.stream()
                .collect(Collectors.groupingBy(
                        PortfolioHoldingSnapshot::sectorName,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioHoldingSnapshot::evaluationAmount, BigDecimal::add)
                ));

        return evaluationAmountBySector.entrySet().stream()
                .map(entry -> new PortfolioSectorSnapshot(
                        entry.getKey(),
                        calculateRate(entry.getValue(), totalEvaluationAmount),
                        entry.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private void saveSectorAnalyses(PortfolioAnalysis analysis, List<PortfolioSectorSnapshot> sectorSnapshots) {
        List<PortfolioSectorAnalysis> sectorAnalyses = sectorSnapshots.stream()
                .map(sector -> PortfolioSectorAnalysis.create(
                        analysis,
                        sector.sectorName(),
                        sector.weight(),
                        sector.evaluationAmount()
                ))
                .toList();
        portfolioSectorAnalysisRepository.saveAll(sectorAnalyses);
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
        List<PortfolioSectorAnalysis> sectorAnalyses =
                portfolioSectorAnalysisRepository.findAllByAnalysisId(analysis.getId());
        return PortfolioAnalysisResponseDto.from(analysis, sectorAnalyses);
    }

    private Portfolio findPortfolio(UUID userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private TradeAssetResponseDto getAssets(UUID userId) {
        TradeAssetResponseDto asset = requestTradeData(() -> tradeServiceClient.getAssets(userId));
        if (asset.totalAsset() == null
                || asset.stockEvaluationAmount() == null
                || asset.totalReturnRate() == null) {
            throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
        }

        return asset;
    }

    private List<TradeAssetHoldingResponseDto> getAssetHoldings(UUID userId) {
        TradeAssetHoldingsResponseDto firstPage = getAssetHoldingPage(userId, DEFAULT_PAGE);
        List<TradeAssetHoldingResponseDto> tradeHoldings = new ArrayList<>(firstPage.content());

        for (int page = 1; page < firstPage.totalPages(); page++) {
            TradeAssetHoldingsResponseDto nextPage = getAssetHoldingPage(userId, page);
            tradeHoldings.addAll(nextPage.content());
        }

        return tradeHoldings.stream()
                .peek(this::validateAssetHolding)
                .toList();
    }

    private TradeAssetHoldingsResponseDto getAssetHoldingPage(UUID userId, int page) {
        TradeAssetHoldingsResponseDto response = requestTradeData(
                () -> tradeServiceClient.getAssetHoldings(
                        userId,
                        page,
                        DEFAULT_SIZE,
                        DEFAULT_ASSET_HOLDINGS_SORT
                )
        );

        if (response.content() == null || response.totalPages() < 0) {
            throw new CustomException(PortfolioErrorCode.TRADE_RESPONSE_INVALID);
        }
        return response;
    }

    private void validateAssetHolding(TradeAssetHoldingResponseDto holding) {
        if (holding == null
                || holding.ticker() == null
                || holding.ticker().isBlank()
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

    private Map<String, StockResponseDto> getStocks(List<TradeAssetHoldingResponseDto> holdings) {
        return holdings.stream()
                .map(holding -> {
                    StockResponseDto stock = requestStockData(
                            () -> stockServiceClient.getStockDetail(holding.ticker())
                    );

                    if (stock.ticker() == null || !holding.ticker().equals(stock.ticker())) {
                        throw new CustomException(PortfolioErrorCode.STOCK_RESPONSE_INVALID);
                    }

                    return stock;
                })
                .collect(Collectors.toMap(
                        StockResponseDto::ticker,
                        stock -> stock,
                        (existing, ignored) -> existing
                ));
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

    private <T> T requestStockData(Supplier<ExternalApiResponseDto<T>> request) {
        try {
            ExternalApiResponseDto<T> response = request.get();
            if (response == null || response.data() == null) {
                throw new CustomException(PortfolioErrorCode.STOCK_RESPONSE_INVALID);
            }
            return response.data();
        } catch (RetryableException exception) {
            throw new CustomException(PortfolioErrorCode.STOCK_SERVICE_TIMEOUT);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                throw new CustomException(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND);
            }
            throw new CustomException(PortfolioErrorCode.STOCK_SERVICE_ERROR);
        }
    }

    private String resolveSectorName(String sectorName) {
        if (sectorName == null || sectorName.isBlank()) {
            return DEFAULT_SECTOR_NAME;
        }
        return sectorName;
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }

        return numerator
                .multiply(HUNDRED)
                .divide(denominator, RATE_SCALE, RoundingMode.HALF_UP);
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
            List<PortfolioSectorSnapshot> sectorAnalyses,
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
                    sectorAnalyses.stream()
                            .map(PortfolioSectorSnapshot::toAiRequest)
                            .toList(),
                    holdings.stream()
                            .map(PortfolioHoldingSnapshot::toAiRequest)
                            .toList(),
                    tendencyAnalysis
            );
        }
    }

    private record PortfolioSectorSnapshot(
            String sectorName,
            BigDecimal weight,
            BigDecimal evaluationAmount
    ) {

        private AiPortfolioSectorAnalysisRequestDto toAiRequest() {
            return new AiPortfolioSectorAnalysisRequestDto(sectorName, weight, evaluationAmount);
        }
    }

    private record PortfolioHoldingSnapshot(
            String ticker,
            String stockName,
            String sectorName,
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
                    sectorName,
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
