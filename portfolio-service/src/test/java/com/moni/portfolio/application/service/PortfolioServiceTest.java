package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.calculator.PortfolioCalculator;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PortfolioService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

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
    @DisplayName("createPortfolio()")
    class CreatePortfolio {

        @Test
        @DisplayName("성공 - 포트폴리오가 없으면 새로 생성한다")
        void success_create() {
            // given
            UUID userId = USER_ID;
            given(portfolioRepository.existsByUserId(userId)).willReturn(false);
            given(portfolioRepository.save(any(Portfolio.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PortfolioCreateResponseDto result = portfolioService.createPortfolio(userId);

            // then
            assertThat(result.userId()).isEqualTo(USER_ID);
            then(portfolioRepository).should().save(any(Portfolio.class));
            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 포트폴리오가 이미 있으면 예외가 발생한다")
        void fail_portfolio_already_exists() {
            // given
            UUID userId = USER_ID;
            given(portfolioRepository.existsByUserId(userId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(userId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS));

            then(portfolioRepository).should().existsByUserId(userId);
            then(portfolioRepository).should(never()).save(any(Portfolio.class));
            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getAssets()")
    class GetAssets {

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            UUID userId = USER_ID;
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(userId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            UUID userId = USER_ID;
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(userId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
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
            UUID userId = USER_ID;
            int page = 0;
            int size = 10;
            String sort = "evaluationAmount,desc";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(userId, page, size, sort))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            UUID userId = USER_ID;
            int page = 0;
            int size = 10;
            String sort = "evaluationAmount,desc";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(userId, page, size, sort))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
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
            UUID userId = USER_ID;
            String ticker = "TICKER-1";
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldingProfitLoss(userId, ticker))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            UUID userId = USER_ID;
            String ticker = "TICKER-1";
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldingProfitLoss(userId, ticker))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
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
            UUID userId = USER_ID;
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getReturns(userId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 외부 서비스 연동이 구현되지 않았으면 예외가 발생한다")
        void fail_external_service_not_implemented() {
            // given
            UUID userId = USER_ID;
            Portfolio portfolio = Portfolio.create(userId);
            given(portfolioRepository.findByUserId(userId)).willReturn(Optional.of(portfolio));

            // when & then
            assertThatThrownBy(() -> portfolioService.getReturns(userId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }
}
