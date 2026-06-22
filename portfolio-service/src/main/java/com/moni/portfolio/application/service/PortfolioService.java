package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.calculator.PortfolioCalculator;
import com.moni.portfolio.application.calculator.model.AccountInput;
import com.moni.portfolio.application.calculator.model.HoldingInput;
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
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingProfitLossResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioReturnsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

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

        Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));

        // TODO: trade-service GET /api/holdings 호출 후 보유 종목 목록 조회
        // TODO: stock-service GET /api/v1/stocks/search 호출 후 현재가/종목 정보 다건 조회(페이징)
        throw new CustomException(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    /** 종목별 손익 조회 로직 */
    public PortfolioHoldingProfitLossResponseDto getHoldingProfitLoss(UUID userId, String ticker) {
        findPortfolio(userId);

        // TODO: trade-service GET /api/holdings/{ticker} 호출 후 단일 보유 종목 조회
        // TODO: trade-service GET /api/v1/trades/history 호출 후 실현손익 계산에 필요한 거래 내역 조회
        // TODO: stock-service GET /api/v1/stocks/{ticker} 호출 후 현재가 조회
        // TODO: trade-service 종목별 실현손익 전용 API 제공 여부 확인
        throw new CustomException(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    /** 수익률 조회 로직 */
    public PortfolioReturnsResponseDto getReturns(UUID userId) {
        findPortfolio(userId);

        // TODO: trade-service GET /api/accounts 호출 후 사용자 자산/예수금 조회
        // TODO: trade-service GET /api/holdings 호출 후 현재 보유 종목 목록 조회
        // TODO: trade-service GET /api/v1/trades/history 호출 후 기간별 보유 수량 변화 계산
        // TODO: stock-service GET /api/v1/stocks/{ticker}/chart 호출 후 가격 시계열 조회
        throw new CustomException(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    private void findPortfolio(UUID userId) {
        portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private AccountInput getAccount(UUID userId) {
        TradeAccountResponseDto account = tradeServiceClient.getAccount(userId).data();

        // TODO: totalInvestment가 포트폴리오 투자 원금과 같은 의미인지 확정 후 매핑 검토
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
}
