package com.moni.portfolio.application.service;

import com.moni.portfolio.application.calculator.PortfolioCalculator;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.ExternalServiceException;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.exception.PortfolioNotFoundException;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PortfolioService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioCalculator portfolioCalculator;

    @Mock
    private TradeServiceClient tradeServiceClient;

    @Mock
    private StockServiceClient stockServiceClient;

    @InjectMocks
    private PortfolioService portfolioService;

    @Nested
    @DisplayName("getAssets()")
    class GetAssets {

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            String userId = "USER-ID-1";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(userId))
                    .isInstanceOfSatisfying(PortfolioNotFoundException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            String userId = "USER-ID-1";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(userId))
                    .isInstanceOfSatisfying(ExternalServiceException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getHoldings()")
    class GetHoldings {

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            String userId = "USER-ID-1";
            int page = 0;
            int size = 10;
            String sort = "evaluationAmount,desc";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(userId, page, size, sort))
                    .isInstanceOfSatisfying(PortfolioNotFoundException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            String userId = "USER-ID-1";
            int page = 0;
            int size = 10;
            String sort = "evaluationAmount,desc";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(userId, page, size, sort))
                    .isInstanceOfSatisfying(ExternalServiceException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getHoldingProfitLoss()")
    class GetHoldingProfitLoss {

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            String userId = "USER-ID-1";
            String ticker = "TICKER-1";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldingProfitLoss(userId, ticker))
                    .isInstanceOfSatisfying(PortfolioNotFoundException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            String userId = "USER-ID-1";
            String ticker = "TICKER-1";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldingProfitLoss(userId, ticker))
                    .isInstanceOfSatisfying(ExternalServiceException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getReturns()")
    class GetReturns {

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            String userId = "USER-ID-1";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getReturns(userId))
                    .isInstanceOfSatisfying(PortfolioNotFoundException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            String userId = "USER-ID-1";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getReturns(userId))
                    .isInstanceOfSatisfying(ExternalServiceException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }
}
