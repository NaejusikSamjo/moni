package com.moni.trade.asset.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.account.application.service.AccountService;
import com.moni.trade.account.presentation.dto.response.AccountResponseDto;
import com.moni.trade.asset.application.calculator.AssetCalculator;
import com.moni.trade.asset.application.calculator.model.AccountInput;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import com.moni.trade.asset.presentation.dto.response.AssetHoldingsResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetResponseDto;
import com.moni.trade.holding.application.service.HoldingService;
import com.moni.trade.holding.presentation.dto.response.HoldingResponseDto;
import com.moni.trade.trade.infrastructure.client.StockServiceClient;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("AssetService 테스트")
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOLDING_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID HOLDING_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final BigDecimal INITIAL_PRINCIPAL_AMOUNT = new BigDecimal("10000000");
    private static final int DEFAULT_SIZE = 10;

    @Mock
    private AccountService accountService;

    @Mock
    private HoldingService holdingService;

    @Mock
    private AssetCalculator assetCalculator;

    @Mock
    private StockServiceClient stockServiceClient;

    @InjectMocks
    private AssetService assetService;

    @Nested
    @DisplayName("getAssets()")
    class GetAssets {

        @Test
        @DisplayName("성공 - 계좌 잔액을 예수금으로 사용해 자산을 계산한다")
        void success_calculate_assets() {
            // given
            AccountResponseDto account = new AccountResponseDto(
                    ACCOUNT_ID, USER_ID, money("9600000"), money("400000")
            );
            HoldingResponseDto firstHolding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            HoldingResponseDto secondHolding = holding(
                    HOLDING_ID_2, "999992", 2, "15000", "30000"
            );

            given(accountService.findAccountByUserId(USER_ID)).willReturn(account);
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(firstHolding), 0, 11));
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(1)))
                    .willReturn(page(List.of(secondHolding), 1, 11));
            given(stockServiceClient.getStock("999991"))
                    .willReturn(success(stock("999991", "11.00")));
            given(stockServiceClient.getStock("999992"))
                    .willReturn(success(stock("999992", "10000")));

            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("999991", 3L, money("10.01"), money("30.02")),
                    new HoldingInput("999992", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> priceInputs = List.of(
                    new PriceInput("999991", "999991 name", money("11.00")),
                    new PriceInput("999992", "999992 name", money("10000"))
            );
            AssetResult calculated = new AssetResult(
                    money("9620033.00"),
                    money("9600000"),
                    money("20033.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("-379967.00"),
                    money("-3.7997"),
                    List.of()
            );
            given(assetCalculator.calculateAssets(
                    new AccountInput(money("9600000"), INITIAL_PRINCIPAL_AMOUNT),
                    holdingInputs,
                    priceInputs
            )).willReturn(calculated);

            // when
            AssetResponseDto result = assetService.getAssets(USER_ID);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("9620033.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("9600000");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("20033.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("10000000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("-379967.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("-3.7997");
            then(holdingService).should().findHoldings(eq(USER_ID), pageNumber(1));
            then(assetCalculator).should().calculateAssets(
                    new AccountInput(money("9600000"), INITIAL_PRINCIPAL_AMOUNT),
                    holdingInputs,
                    priceInputs
            );
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 현재가를 조회하지 않는다")
        void success_empty_holdings() {
            // given
            AccountResponseDto account = new AccountResponseDto(
                    ACCOUNT_ID, USER_ID, money("10000000"), BigDecimal.ZERO
            );
            AssetResult calculated = new AssetResult(
                    money("10000000.00"),
                    money("10000000"),
                    money("0.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("0.00"),
                    money("0.0000"),
                    List.of()
            );

            given(accountService.findAccountByUserId(USER_ID)).willReturn(account);
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(), 0, 0));
            given(assetCalculator.calculateAssets(
                    new AccountInput(money("10000000"), INITIAL_PRINCIPAL_AMOUNT),
                    List.of(),
                    List.of()
            )).willReturn(calculated);

            // when
            AssetResponseDto result = assetService.getAssets(USER_ID);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("0.00");
            verifyNoInteractions(stockServiceClient);
        }

        @Test
        @DisplayName("실패 - 계좌 잔액이 없으면 Trade 응답 오류가 발생한다")
        void fail_account_balance_missing() {
            // given
            AccountResponseDto account = new AccountResponseDto(
                    ACCOUNT_ID, USER_ID, null, BigDecimal.ZERO
            );
            given(accountService.findAccountByUserId(USER_ID)).willReturn(account);

            // when & then
            assertThatThrownBy(() -> assetService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.TRADE_RESPONSE_INVALID));

            verifyNoInteractions(holdingService, stockServiceClient, assetCalculator);
        }
    }

    @Nested
    @DisplayName("getHoldings()")
    class GetHoldings {

        @Test
        @DisplayName("성공 - 평가금액 기준으로 정렬한 뒤 요청한 페이지를 반환한다")
        void success_sort_and_page_holdings() {
            // given
            HoldingResponseDto firstHolding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            HoldingResponseDto secondHolding = holding(
                    HOLDING_ID_2, "999992", 2, "15000", "30000"
            );
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(firstHolding, secondHolding), 0, 2));
            given(stockServiceClient.getStock("999991"))
                    .willReturn(success(stock("999991", "11.00")));
            given(stockServiceClient.getStock("999992"))
                    .willReturn(success(stock("999992", "10000")));

            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("999991", 3L, money("10.01"), money("30.02")),
                    new HoldingInput("999992", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> priceInputs = List.of(
                    new PriceInput("999991", "999991 name", money("11.00")),
                    new PriceInput("999992", "999992 name", money("10000"))
            );
            List<HoldingResult> holdingResults = List.of(
                    holdingResult("999991", "33.00"),
                    holdingResult("999992", "20000.00")
            );
            given(assetCalculator.calculateHoldings(holdingInputs, priceInputs))
                    .willReturn(holdingResults);

            // when
            AssetHoldingsResponseDto result = assetService.getHoldings(
                    USER_ID, 0, 1, "evaluationAmount,desc"
            );

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().ticker()).isEqualTo("999992");
            assertThat(result.content().getFirst().stockName()).isEqualTo("999992 name");
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.totalPages()).isEqualTo(2);
            assertThat(result.sort()).isEqualTo("evaluationAmount,desc");
        }

        @Test
        @DisplayName("성공 - 잘못된 페이지 조건과 정렬 조건은 기본값으로 보정한다")
        void success_resolve_invalid_page_options() {
            // given
            HoldingResponseDto holding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStock("999991"))
                    .willReturn(success(stock("999991", "11.00")));
            given(assetCalculator.calculateHoldings(any(), any()))
                    .willReturn(List.of(holdingResult("999991", "33.00")));

            // when
            AssetHoldingsResponseDto result = assetService.getHoldings(
                    USER_ID, -1, 100, "unknown"
            );

            // then
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.sort()).isEqualTo("evaluationAmount,desc");
            assertThat(result.content()).extracting("ticker").containsExactly("999991");
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("실패 - Holding 페이지 응답 내용이 없으면 Trade 응답 오류가 발생한다")
        void fail_holding_page_content_missing() {
            // given
            PageRes<HoldingResponseDto> invalidPage = mock(PageRes.class);
            given(invalidPage.getContent()).willReturn(null);
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0))).willReturn(invalidPage);

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.TRADE_RESPONSE_INVALID));

            verifyNoInteractions(stockServiceClient, assetCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 응답의 ticker가 요청한 ticker와 다르면 Stock 응답 오류가 발생한다")
        void fail_stock_ticker_mismatch() {
            // given
            HoldingResponseDto holding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStock("999991"))
                    .willReturn(success(stock("999992", "11.00")));

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_RESPONSE_INVALID));

            verifyNoInteractions(assetCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 응답의 현재가가 0 이하이면 현재가 없음 오류가 발생한다")
        void fail_stock_price_not_positive() {
            // given
            HoldingResponseDto holding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStock("999991"))
                    .willReturn(success(stock("999991", "0.00")));

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(assetCalculator);
        }

        @Test
        @DisplayName("실패 - Stock 404 예외는 현재가 없음 오류로 변환한다")
        void fail_stock_not_found() {
            // given
            HoldingResponseDto holding = holding(
                    HOLDING_ID_1, "999991", 3, "10.01", "30.02"
            );
            FeignException exception = mock(FeignException.class);
            given(exception.status()).willReturn(404);
            given(holdingService.findHoldings(eq(USER_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStock("999991")).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, customException ->
                            assertThat(customException.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(assetCalculator);
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private HoldingResponseDto holding(
            UUID id,
            String ticker,
            int quantity,
            String averagePrice,
            String totalAmount
    ) {
        return new HoldingResponseDto(
                id,
                ticker,
                quantity,
                money(averagePrice),
                money(totalAmount)
        );
    }

    private StockPriceResponseDto stock(String ticker, String price) {
        return new StockPriceResponseDto(ticker, ticker + " name", money(price));
    }

    private HoldingResult holdingResult(String ticker, String evaluationAmount) {
        return new HoldingResult(
                ticker,
                ticker + " name",
                1L,
                money("10000"),
                money(evaluationAmount),
                money(evaluationAmount),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private <T> ExternalApiResponseDto<T> success(T data) {
        return new ExternalApiResponseDto<>(200, "SUCCESS", data, null);
    }

    private <T> PageRes<T> page(List<T> content, int pageNumber, long totalElements) {
        return new PageRes<>(new PageImpl<>(
                content,
                PageRequest.of(pageNumber, DEFAULT_SIZE),
                totalElements
        ));
    }

    private Pageable pageNumber(int pageNumber) {
        return argThat(pageable ->
                pageable != null
                        && pageable.getPageNumber() == pageNumber
                        && pageable.getPageSize() == DEFAULT_SIZE
        );
    }
}
