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
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.StockResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAccountResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradePageResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAssetResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PortfolioService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOLDING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID HOLDING_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final BigDecimal INITIAL_PRINCIPAL_AMOUNT = new BigDecimal("10000000");

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
            given(portfolioRepository.existsByUserId(USER_ID)).willReturn(false);
            given(portfolioRepository.save(any(Portfolio.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            PortfolioCreateResponseDto result = portfolioService.createPortfolio(USER_ID);

            // then
            assertThat(result.userId()).isEqualTo(USER_ID);
            then(portfolioRepository).should().save(any(Portfolio.class));
            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - 포트폴리오가 이미 있으면 예외가 발생한다")
        void fail_portfolio_already_exists() {
            // given
            given(portfolioRepository.existsByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS));

            then(portfolioRepository).should().existsByUserId(USER_ID);
            then(portfolioRepository).should(never()).save(any(Portfolio.class));
            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getAssets()")
    class GetAssets {

        @Test
        @DisplayName("성공 - 전체 보유 페이지와 현재가를 조회해 자산을 계산한다")
        void success_calculate_assets() {
            // given
            givenPortfolioExists();

            TradeAccountResponseDto account = new TradeAccountResponseDto(
                    ACCOUNT_ID, USER_ID, money("9600000"), money("400000")
            );
            TradeHoldingResponseDto firstHolding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 3, "10.01", "30.02"
            );
            TradeHoldingResponseDto secondHolding = tradeHolding(
                    HOLDING_ID_2, "TICKER-2", 2, "15000", "30000"
            );

            given(tradeServiceClient.getAccount(USER_ID)).willReturn(success(account));
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(firstHolding), 0, 2, false)));
            given(tradeServiceClient.getHoldings(USER_ID, 1, 10))
                    .willReturn(success(page(List.of(secondHolding), 1, 2, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(stock("TICKER-1", "11.00")));
            given(stockServiceClient.getStockDetail("TICKER-2"))
                    .willReturn(success(stock("TICKER-2", "10000")));

            AccountInput accountInput = new AccountInput(money("9600000"), INITIAL_PRINCIPAL_AMOUNT);
            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("TICKER-1", 3L, money("10.01"), money("30.02")),
                    new HoldingInput("TICKER-2", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> priceInputs = List.of(
                    new PriceInput("TICKER-1", money("11.00")),
                    new PriceInput("TICKER-2", money("10000"))
            );
            PortfolioAssetResult calculated = new PortfolioAssetResult(
                    money("9620033.00"),
                    money("9600000"),
                    money("20033.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("-379967.00"),
                    money("-3.7997"),
                    List.of()
            );
            given(portfolioCalculator.calculateAssets(accountInput, holdingInputs, priceInputs))
                    .willReturn(calculated);

            // when
            PortfolioAssetResponseDto result = portfolioService.getAssets(USER_ID);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("9620033.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("9600000");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("20033.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("10000000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("-379967.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("-3.7997");
            then(tradeServiceClient).should().getHoldings(USER_ID, 1, 10);
            then(portfolioCalculator).should().calculateAssets(accountInput, holdingInputs, priceInputs);
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 현재가를 조회하지 않는다")
        void success_empty_holdings() {
            // given
            givenPortfolioExists();
            TradeAccountResponseDto account = new TradeAccountResponseDto(
                    ACCOUNT_ID,
                    USER_ID,
                    money("10000000"),
                    BigDecimal.ZERO
            );
            AccountInput accountInput = new AccountInput(money("10000000"), INITIAL_PRINCIPAL_AMOUNT);
            PortfolioAssetResult calculated = new PortfolioAssetResult(
                    money("10000000.00"),
                    money("10000000"),
                    money("0.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("0.00"),
                    money("0.0000"),
                    List.of()
            );

            given(tradeServiceClient.getAccount(USER_ID)).willReturn(success(account));
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(), 0, 0, true)));
            given(portfolioCalculator.calculateAssets(accountInput, List.of(), List.of()))
                    .willReturn(calculated);

            // when
            PortfolioAssetResponseDto result = portfolioService.getAssets(USER_ID);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("0.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("0.00");
            verifyNoInteractions(stockServiceClient);
        }

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 계좌 조회 중 Feign 예외가 발생하면 Trade 연동 오류로 변환한다")
        void fail_trade_account_feign_error() {
            // given
            givenPortfolioExists();
            given(tradeServiceClient.getAccount(USER_ID)).willThrow(mock(FeignException.class));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.TRADE_SERVICE_ERROR));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 계좌 응답 데이터가 없으면 Trade 응답 오류가 발생한다")
        void fail_trade_account_data_missing() {
            // given
            givenPortfolioExists();
            given(tradeServiceClient.getAccount(USER_ID)).willReturn(success(null));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.TRADE_RESPONSE_INVALID));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 계좌 잔액이 없으면 Trade 응답 오류가 발생한다")
        void fail_trade_account_balance_missing() {
            // given
            givenPortfolioExists();
            TradeAccountResponseDto account = new TradeAccountResponseDto(
                    ACCOUNT_ID, USER_ID, null, BigDecimal.ZERO
            );
            given(tradeServiceClient.getAccount(USER_ID)).willReturn(success(account));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.TRADE_RESPONSE_INVALID));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 계좌가 없으면 투자 계좌 없음 오류가 발생한다")
        void fail_trade_account_not_found() {
            // given
            givenPortfolioExists();
            FeignException exception = mock(FeignException.class);
            given(exception.status()).willReturn(404);
            given(tradeServiceClient.getAccount(USER_ID)).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, customException ->
                            assertThat(customException.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.TRADE_ACCOUNT_NOT_FOUND));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 응답 시간이 초과되면 Trade 타임아웃 오류가 발생한다")
        void fail_trade_timeout() {
            // given
            givenPortfolioExists();
            given(tradeServiceClient.getAccount(USER_ID)).willThrow(mock(RetryableException.class));

            // when & then
            assertThatThrownBy(() -> portfolioService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.TRADE_SERVICE_TIMEOUT));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }
    }

    @Nested
    @DisplayName("getHoldings()")
    class GetHoldings {

        @Test
        @DisplayName("성공 - 누적 매수 금액과 현재가를 전달하고 평가금액 내림차순으로 페이지를 반환한다")
        void success_sort_desc_and_paginate() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto firstHolding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 3, "10.01", "30.02"
            );
            TradeHoldingResponseDto secondHolding = tradeHolding(
                    HOLDING_ID_2, "TICKER-2", 2, "15000", "30000"
            );
            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("TICKER-1", 3L, money("10.01"), money("30.02")),
                    new HoldingInput("TICKER-2", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> priceInputs = List.of(
                    new PriceInput("TICKER-1", money("11.00")),
                    new PriceInput("TICKER-2", money("10000"))
            );
            List<HoldingResult> calculated = List.of(
                    holdingResult("TICKER-1", 3L, "10.01", "11.00", "33.00", "2.98", "9.9267", "0.1647"),
                    holdingResult("TICKER-2", 2L, "15000", "10000", "20000.00", "-10000.00", "-33.3333", "99.8353")
            );

            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(firstHolding, secondHolding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(stock("TICKER-1", "11.00")));
            given(stockServiceClient.getStockDetail("TICKER-2"))
                    .willReturn(success(stock("TICKER-2", "10000")));
            given(portfolioCalculator.calculateHoldings(holdingInputs, priceInputs)).willReturn(calculated);

            // when
            PortfolioHoldingsResponseDto result = portfolioService.getHoldings(
                    USER_ID, 0, 1, "evaluationAmount,desc"
            );

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().ticker()).isEqualTo("TICKER-2");
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.totalPages()).isEqualTo(2);
            assertThat(result.sort()).isEqualTo("evaluationAmount,desc");
            then(portfolioCalculator).should().calculateHoldings(holdingInputs, priceInputs);
        }

        @Test
        @DisplayName("성공 - 잘못된 페이지 조건은 기본값으로 보정한다")
        void success_resolve_invalid_query() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            HoldingInput holdingInput = new HoldingInput(
                    "TICKER-1", 1L, money("10000"), money("10000")
            );
            PriceInput priceInput = new PriceInput("TICKER-1", money("12000"));
            HoldingResult calculated = holdingResult(
                    "TICKER-1", 1L, "10000", "12000", "12000.00", "2000.00", "20.0000", "100.0000"
            );

            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(stock("TICKER-1", "12000")));
            given(portfolioCalculator.calculateHoldings(List.of(holdingInput), List.of(priceInput)))
                    .willReturn(List.of(calculated));

            // when
            PortfolioHoldingsResponseDto result = portfolioService.getHoldings(
                    USER_ID, -1, 51, "profitRate,asc"
            );

            // then
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.sort()).isEqualTo("evaluationAmount,desc");
            assertThat(result.content()).extracting("ticker").containsExactly("TICKER-1");
        }

        @Test
        @DisplayName("성공 - 요청 페이지가 범위를 벗어나면 빈 목록을 반환한다")
        void success_page_out_of_range() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            HoldingInput holdingInput = new HoldingInput(
                    "TICKER-1", 1L, money("10000"), money("10000")
            );
            PriceInput priceInput = new PriceInput("TICKER-1", money("12000"));
            HoldingResult calculated = holdingResult(
                    "TICKER-1", 1L, "10000", "12000", "12000.00", "2000.00", "20.0000", "100.0000"
            );

            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(stock("TICKER-1", "12000")));
            given(portfolioCalculator.calculateHoldings(List.of(holdingInput), List.of(priceInput)))
                    .willReturn(List.of(calculated));

            // when
            PortfolioHoldingsResponseDto result = portfolioService.getHoldings(
                    USER_ID, 2, 10, "evaluationAmount,asc"
            );

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.page()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.sort()).isEqualTo("evaluationAmount,asc");
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 빈 페이지를 반환한다")
        void success_empty_holdings() {
            // given
            givenPortfolioExists();
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(), 0, 0, true)));
            given(portfolioCalculator.calculateHoldings(List.of(), List.of())).willReturn(List.of());

            // when
            PortfolioHoldingsResponseDto result = portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            );

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.totalPages()).isZero();
            verifyNoInteractions(stockServiceClient);
        }

        @Test
        @DisplayName("실패 - 포트폴리오가 없으면 예외가 발생한다")
        void fail_portfolio_not_found() {
            // given
            given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

            verifyNoInteractions(tradeServiceClient, stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Trade 보유 종목의 필수값이 없으면 Trade 응답 오류가 발생한다")
        void fail_trade_holding_field_missing() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = new TradeHoldingResponseDto(
                    HOLDING_ID_1, null, 1, money("10000"), money("10000")
            );
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.TRADE_RESPONSE_INVALID));

            verifyNoInteractions(stockServiceClient, portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 조회 중 Feign 예외가 발생하면 Stock 연동 오류로 변환한다")
        void fail_stock_feign_error() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willThrow(mock(FeignException.class));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_SERVICE_ERROR));

            verifyNoInteractions(portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 응답 시간이 초과되면 Stock 타임아웃 오류가 발생한다")
        void fail_stock_timeout() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willThrow(mock(RetryableException.class));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_SERVICE_TIMEOUT));

            verifyNoInteractions(portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Stock에서 종목을 찾지 못하면 현재가 조회 오류가 발생한다")
        void fail_stock_not_found() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            FeignException exception = mock(FeignException.class);
            given(exception.status()).willReturn(404);
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1")).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, customException ->
                    assertThat(customException.getErrorCode())
                            .isEqualTo(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 응답 종목이 요청 종목과 다르면 Stock 응답 오류가 발생한다")
        void fail_stock_ticker_mismatch() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(stock("TICKER-2", "12000")));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_RESPONSE_INVALID));

            verifyNoInteractions(portfolioCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 현재가가 없으면 현재가 조회 오류가 발생한다")
        void fail_stock_price_missing() {
            // given
            givenPortfolioExists();
            TradeHoldingResponseDto holding = tradeHolding(
                    HOLDING_ID_1, "TICKER-1", 1, "10000", "10000"
            );
            given(tradeServiceClient.getHoldings(USER_ID, 0, 10))
                    .willReturn(success(page(List.of(holding), 0, 1, true)));
            given(stockServiceClient.getStockDetail("TICKER-1"))
                    .willReturn(success(new StockResponseDto("TICKER-1", "종목 1", null)));

            // when & then
            assertThatThrownBy(() -> portfolioService.getHoldings(
                    USER_ID, 0, 10, "evaluationAmount,desc"
            )).isInstanceOfSatisfying(CustomException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(portfolioCalculator);
        }
    }

    private void givenPortfolioExists() {
        given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.of(Portfolio.create(USER_ID)));
    }

    private TradeHoldingResponseDto tradeHolding(
            UUID id,
            String ticker,
            int quantity,
            String averagePrice,
            String totalAmount
    ) {
        return new TradeHoldingResponseDto(
                id,
                ticker,
                quantity,
                money(averagePrice),
                money(totalAmount)
        );
    }

    private StockResponseDto stock(String ticker, String price) {
        return new StockResponseDto(ticker, ticker + " 이름", money(price));
    }

    private TradePageResponseDto<TradeHoldingResponseDto> page(
            List<TradeHoldingResponseDto> content,
            int pageNumber,
            int totalPages,
            boolean last
    ) {
        return new TradePageResponseDto<>(content, pageNumber, 10, content.size(), totalPages, last);
    }

    private HoldingResult holdingResult(
            String ticker,
            long quantity,
            String averagePrice,
            String currentPrice,
            String evaluationAmount,
            String profitLoss,
            String profitRate,
            String weight
    ) {
        return new HoldingResult(
                ticker,
                quantity,
                money(averagePrice),
                money(currentPrice),
                money(evaluationAmount),
                money(profitLoss),
                money(profitRate),
                money(weight)
        );
    }

    private <T> ExternalApiResponseDto<T> success(T data) {
        return new ExternalApiResponseDto<>(200, "SUCCESS", data, null);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
