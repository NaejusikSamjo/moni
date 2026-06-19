package com.moni.portfolio.application.service;

import com.moni.portfolio.application.calculator.PortfolioCalculator;
import com.moni.portfolio.application.calculator.model.AccountInput;
import com.moni.portfolio.application.calculator.model.HoldingInput;
import com.moni.portfolio.application.calculator.model.PortfolioAssetResult;
import com.moni.portfolio.application.calculator.model.PriceInput;
import com.moni.portfolio.domain.exception.ExternalServiceException;
import com.moni.portfolio.domain.exception.PortfolioNotFoundException;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.presentation.dto.response.PortfolioAssetResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingProfitLossResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioReturnsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // TODO: trade-service 보유 종목과 stock-service 현재가/종목 정보를 조회한 뒤 페이징/정렬 적용
        throw new ExternalServiceException();
    }

    /** 종목별 손익 조회 로직 */
    public PortfolioHoldingProfitLossResponseDto getHoldingProfitLoss(UUID userId, String ticker) {
        findPortfolio(userId);

        // TODO: trade-service 단일 보유 종목/실현손익과 stock-service 현재가를 조회한 뒤 손익 계산
        throw new ExternalServiceException();
    }

    /** 수익률 조회 로직 */
    public PortfolioReturnsResponseDto getReturns(UUID userId) {
        findPortfolio(userId);

        // TODO: trade-service 보유 수량 변화와 stock-service 가격 시계열을 조회한 뒤 수익률 계산
        throw new ExternalServiceException();
    }

    private void findPortfolio(UUID userId) {
        portfolioRepository.findByUserId(userId)
                .orElseThrow(PortfolioNotFoundException::new);
    }

    private AccountInput getAccount(UUID userId) {
        // TODO: trade-service 계좌 조회 API 확정 후 TradeServiceClient 호출로 구현
        throw new ExternalServiceException();
    }

    private List<HoldingInput> getHoldings(UUID userId) {
        // TODO: trade-service 보유 종목 조회 API 확정 후 TradeServiceClient 호출로 구현
        throw new ExternalServiceException();
    }

    private List<PriceInput> getPrices(List<HoldingInput> holdings) {
        // TODO: stock-service 현재가 조회 API 확정 후 StockServiceClient 호출로 구현
        throw new ExternalServiceException();
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
