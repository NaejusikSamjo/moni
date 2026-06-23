package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.calculator.PortfolioCalculator;
import com.moni.portfolio.application.calculator.model.AccountInput;
import com.moni.portfolio.application.calculator.model.HoldingInput;
import com.moni.portfolio.application.calculator.model.HoldingResult;
import com.moni.portfolio.application.calculator.model.PortfolioAssetResult;
import com.moni.portfolio.application.calculator.model.PriceInput;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.dto.response.StockResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAccountResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradePageResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAssetResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final String DEFAULT_SORT = "evaluationAmount,desc";
    private static final String EVALUATION_AMOUNT_ASC = "evaluationAmount,asc";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioCalculator portfolioCalculator;
    private final TradeServiceClient tradeServiceClient;
    private final StockServiceClient stockServiceClient;

    /** 포트폴리오 생성 로직 */
    @Transactional
    public PortfolioCreateResponseDto createPortfolio(UUID userId) {
        if (portfolioRepository.existsByUserId(userId)) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        Portfolio portfolio = Portfolio.create(userId);

        return PortfolioCreateResponseDto.from(portfolioRepository.save(portfolio));
    }

    /** 자산 조회 로직 */
    public PortfolioAssetResponseDto getAssets(UUID userId) {
        findPortfolio(userId);

        AccountInput account = getAccount(userId);
        List<HoldingInput> holdings = getHoldings(userId);
        List<PriceInput> prices = getPrices(holdings);
        PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

        return PortfolioAssetResponseDto.from(result);
    }

    /** 보유 종목 현황 조회 로직 */
    public PortfolioHoldingsResponseDto getHoldings(UUID userId, int page, int size, String sort) {
        findPortfolio(userId);

        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        String resolvedSort = resolveSort(sort);

        // Trade 보유 종목 전체를 조회한 뒤 각 ticker의 현재가를 Stock에서 조회
        List<HoldingInput> holdings = getHoldings(userId);
        List<PriceInput> prices = getPrices(holdings);

        // 평가금액, 평가손익, 수익률과 비중은 페이지를 나누기 전에 전체 종목 기준으로 계산
        List<HoldingResult> holdingResults = portfolioCalculator.calculateHoldings(holdings, prices);

        // 계산 결과의 평가금액을 기준으로 오름차순 또는 기본 내림차순 정렬
        Comparator<HoldingResult> comparator = Comparator.comparing(HoldingResult::evaluationAmount);
        if (DEFAULT_SORT.equals(resolvedSort)) {
            comparator = comparator.reversed();
        }

        List<HoldingResult> sortedHoldings = holdingResults.stream()
                .sorted(comparator)
                .toList();

        long totalElements = sortedHoldings.size();
        int totalPages = calculateTotalPages(totalElements, resolvedSize);
        long fromIndex = (long) resolvedPage * resolvedSize;

        // 전체 결과의 정렬이 끝난 뒤 요청한 페이지 범위만 응답 DTO로 변환
        List<PortfolioHoldingResponseDto> content;
        if (fromIndex >= totalElements) {
            content = List.of();
        } else {
            int from = (int) fromIndex;
            int to = Math.min(from + resolvedSize, sortedHoldings.size());
            content = sortedHoldings.subList(from, to).stream()
                    .map(PortfolioHoldingResponseDto::from)
                    .toList();
        }

        return new PortfolioHoldingsResponseDto(
                content,
                resolvedPage,
                resolvedSize,
                totalElements,
                totalPages,
                resolvedSort
        );
    }

    private void findPortfolio(UUID userId) {
        portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private AccountInput getAccount(UUID userId) {
        TradeAccountResponseDto account = tradeServiceClient.getAccount(userId).data();
        return new AccountInput(account.balance(), account.totalInvestment());
    }

    private List<HoldingInput> getHoldings(UUID userId) {
        TradePageResponseDto<TradeHoldingResponseDto> firstPage =
                tradeServiceClient.getHoldings(userId, DEFAULT_PAGE, DEFAULT_SIZE).data();
        List<TradeHoldingResponseDto> tradeHoldings = new ArrayList<>(firstPage.content());

        for (int page = 1; page < firstPage.totalPages(); page++) {
            TradePageResponseDto<TradeHoldingResponseDto> nextPage =
                    tradeServiceClient.getHoldings(userId, page, DEFAULT_SIZE).data();
            tradeHoldings.addAll(nextPage.content());
        }

        return tradeHoldings.stream()
                .map(holding -> new HoldingInput(
                        holding.ticker(),
                        holding.quantity().longValue(),
                        holding.averagePrice()
                ))
                .toList();
    }

    private List<PriceInput> getPrices(List<HoldingInput> holdings) {
        return holdings.stream()
                .map(holding -> {
                    StockResponseDto stock = stockServiceClient.getStockDetail(holding.ticker()).data();
                    return new PriceInput(stock.ticker(), stock.price());
                })
                .toList();
    }

    /** page는 음수일 경우 기본 페이지로 보정 */
    private int resolvePage(int page) {
        if (page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /** size는 1 미만 또는 최대 허용값 초과 시 기본 크기로 보정 */
    private int resolveSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    /** 지원하지 않는 정렬 조건은 기본 정렬로 보정 */
    private String resolveSort(String sort) {
        if (EVALUATION_AMOUNT_ASC.equalsIgnoreCase(sort)) {
            return EVALUATION_AMOUNT_ASC;
        }
        return DEFAULT_SORT;
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
