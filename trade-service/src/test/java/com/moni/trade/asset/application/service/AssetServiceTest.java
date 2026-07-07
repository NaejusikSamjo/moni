package com.moni.trade.asset.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.asset.application.calculator.AssetCalculator;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.application.calculator.model.StockSummaryResult;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import com.moni.trade.asset.presentation.dto.response.AssetAnalysisSnapshotResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetHoldingsResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetResponseDto;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.trade.infrastructure.client.StockServiceClient;
import com.moni.trade.trade.infrastructure.client.dto.BatchStockRequestDto;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private static final BigDecimal INITIAL_PRINCIPAL_AMOUNT = new BigDecimal("10000000");
    private static final int DEFAULT_SIZE = 10;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

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
            Account account = accountWithBalance("9600000");
            Holding firstHolding = holding("999991", 3, "10.01", "30.02");
            Holding secondHolding = holding("999992", 2, "15000", "30000");

            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(firstHolding), 0, 11));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(1)))
                    .willReturn(page(List.of(secondHolding), 1, 11));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991", "999992"))))
                    .willReturn(success(List.of(
                            stock("999991", "11.00"),
                            stock("999992", "10000")
                    )));

            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("999991", money("3"), money("10.01"), money("30.02")),
                    new HoldingInput("999992", money("2"), money("15000"), money("30000"))
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
                    money("3.00"),
                    money("0.0100")
            );
            given(assetCalculator.calculateAssets(
                    money("9600000"),
                    INITIAL_PRINCIPAL_AMOUNT,
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
            then(holdingRepository).should().findByAccountId(eq(ACCOUNT_ID), pageNumber(1));
            then(assetCalculator).should().calculateAssets(
                    money("9600000"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    holdingInputs,
                    priceInputs
            );
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 현재가를 조회하지 않는다")
        void success_empty_holdings() {
            // given
            Account account = accountWithBalance("10000000");
            AssetResult calculated = new AssetResult(
                    money("10000000.00"),
                    money("10000000"),
                    money("0.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("0.00"),
                    money("0.0000"),
                    money("0.00"),
                    money("0.0000")
            );

            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(), 0, 0));
            given(assetCalculator.calculateAssets(
                    money("10000000"),
                    INITIAL_PRINCIPAL_AMOUNT,
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
        @DisplayName("실패 - 계좌가 없으면 계좌 없음 오류가 발생한다")
        void fail_account_not_found() {
            // given
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> assetService.getAssets(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND));

            verifyNoInteractions(holdingRepository, stockServiceClient, assetCalculator);
        }
    }

    @Nested
    @DisplayName("getHoldings()")
    class GetHoldings {

        @Test
        @DisplayName("성공 - 평가금액 기준으로 정렬한 뒤 요청한 페이지를 반환한다")
        void success_sort_and_page_holdings() {
            // given
            Account account = account();
            Holding firstHolding = holding("999991", 3, "10.01", "30.02");
            Holding secondHolding = holding("999992", 2, "15000", "30000");
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(firstHolding, secondHolding), 0, 2));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991", "999992"))))
                    .willReturn(success(List.of(
                            stock("999991", "11.00"),
                            stock("999992", "10000")
                    )));

            List<HoldingInput> holdingInputs = List.of(
                    new HoldingInput("999991", money("3"), money("10.01"), money("30.02")),
                    new HoldingInput("999992", money("2"), money("15000"), money("30000"))
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
            given(assetCalculator.calculateStockSummary(holdingInputs, holdingResults))
                    .willReturn(new StockSummaryResult(money("3.00"), money("0.0100")));

            // when
            AssetHoldingsResponseDto result = assetService.getHoldings(
                    USER_ID, 0, 1, "evaluationAmount,desc"
            );

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.stockProfitLoss()).isEqualByComparingTo("3.00");
            assertThat(result.stockReturnRate()).isEqualByComparingTo("0.0100");
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
            Account account = account();
            Holding holding = holding("999991", 3, "10.01", "30.02");
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991"))))
                    .willReturn(success(List.of(stock("999991", "11.00"))));
            given(assetCalculator.calculateHoldings(any(), any()))
                    .willReturn(List.of(holdingResult("999991", "33.00")));
            given(assetCalculator.calculateStockSummary(any(), any()))
                    .willReturn(new StockSummaryResult(money("2.98"), money("9.9267")));

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
            assertThat(result.stockProfitLoss()).isEqualByComparingTo("2.98");
            assertThat(result.stockReturnRate()).isEqualByComparingTo("9.9267");
            assertThat(result.content()).extracting("ticker").containsExactly("999991");
        }

        @Test
        @DisplayName("실패 - Stock 응답의 ticker가 요청한 ticker와 다르면 Stock 응답 오류가 발생한다")
        void fail_stock_ticker_mismatch() {
            // given
            Account account = account();
            Holding holding = holding("999991", 3, "10.01", "30.02");
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991"))))
                    .willReturn(success(List.of(stock("999992", "11.00"))));

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
            Account account = account();
            Holding holding = holding("999991", 3, "10.01", "30.02");
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991"))))
                    .willReturn(success(List.of(stock("999991", "0.00"))));

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(assetCalculator);
        }

        @Test
        @DisplayName("실패 - Stock batch 응답에 요청 종목이 없으면 현재가 없음 오류가 발생한다")
        void fail_stock_not_found() {
            // given
            Account account = account();
            Holding holding = holding("999991", 3, "10.01", "30.02");
            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(List.of(holding), 0, 1));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(List.of("999991"))))
                    .willReturn(success(List.of()));

            // when & then
            assertThatThrownBy(() -> assetService.getHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .isInstanceOfSatisfying(CustomException.class, customException ->
                            assertThat(customException.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));

            verifyNoInteractions(assetCalculator);
        }
    }

    @Nested
    @DisplayName("getAnalysisSnapshot()")
    class GetAnalysisSnapshot {

        @Test
        @DisplayName("성공 - 자산 요약과 보유 비중 상위 10개 종목을 반환한다")
        void success_get_analysis_snapshot() {
            // given
            Account account = accountWithBalance("9600000");
            List<Holding> tradeHoldings = new ArrayList<>();
            List<HoldingInput> holdingInputs = new ArrayList<>();
            List<PriceInput> priceInputs = new ArrayList<>();
            List<HoldingResult> holdingResults = new ArrayList<>();
            List<StockPriceResponseDto> stocks = new ArrayList<>();

            for (int index = 1; index <= 11; index++) {
                String ticker = "9000" + index;
                tradeHoldings.add(holding(ticker, 1, "1000", "1000"));
                holdingInputs.add(new HoldingInput(ticker, money("1"), money("1000"), money("1000")));
                priceInputs.add(new PriceInput(ticker, ticker + " name", money("1000")));
                holdingResults.add(holdingResult(ticker, "1000.00", String.valueOf(index)));
                stocks.add(stock(ticker, "1000"));
            }

            AssetResult assetResult = new AssetResult(
                    money("10700000.00"),
                    money("9600000"),
                    money("1100000.00"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    money("700000.00"),
                    money("7.0000"),
                    money("100000.00"),
                    money("10.0000")
            );

            given(accountRepository.findByUserId(USER_ID)).willReturn(Optional.of(account));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(0)))
                    .willReturn(page(tradeHoldings.subList(0, 10), 0, 11));
            given(holdingRepository.findByAccountId(eq(ACCOUNT_ID), pageNumber(1)))
                    .willReturn(page(tradeHoldings.subList(10, 11), 1, 11));
            given(stockServiceClient.getStocks(new BatchStockRequestDto(
                    holdingInputs.stream()
                            .map(HoldingInput::ticker)
                            .toList()
            ))).willReturn(success(stocks));
            given(assetCalculator.calculateAssets(
                    money("9600000"),
                    INITIAL_PRINCIPAL_AMOUNT,
                    holdingInputs,
                    priceInputs
            )).willReturn(assetResult);
            given(assetCalculator.calculateHoldings(holdingInputs, priceInputs))
                    .willReturn(holdingResults);

            // when
            AssetAnalysisSnapshotResponseDto result = assetService.getAnalysisSnapshot(USER_ID);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10700000.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("9600000");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("1100000.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("10000000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("700000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("7.0000");
            assertThat(result.stockProfitLoss()).isEqualByComparingTo("100000.00");
            assertThat(result.stockReturnRate()).isEqualByComparingTo("10.0000");
            assertThat(result.holdings()).hasSize(10);
            assertThat(result.holdings()).extracting("ticker")
                    .containsExactly(
                            "900011", "900010", "90009", "90008", "90007",
                            "90006", "90005", "90004", "90003", "90002"
                    );
        }
    }

    private BigDecimal money(String value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value);
    }

    private Account account() {
        Account account = mock(Account.class);
        given(account.getId()).willReturn(ACCOUNT_ID);
        return account;
    }

    private Account accountWithBalance(String balance) {
        Account account = account();
        given(account.getBalance()).willReturn(money(balance));
        return account;
    }

    private Holding holding(
            String ticker,
            int quantity,
            String averagePrice,
            String totalAmount
    ) {
        Holding holding = mock(Holding.class);
        given(holding.getTicker()).willReturn(ticker);
        given(holding.getQuantity()).willReturn(BigDecimal.valueOf(quantity));
        given(holding.getAveragePrice()).willReturn(money(averagePrice));
        given(holding.getTotalAmount()).willReturn(money(totalAmount));
        return holding;
    }

    private StockPriceResponseDto stock(String ticker, String price) {
        return new StockPriceResponseDto(ticker, ticker + " name", money(price));
    }

    private HoldingResult holdingResult(String ticker, String evaluationAmount) {
        return holdingResult(ticker, evaluationAmount, "0.0000");
    }

    private HoldingResult holdingResult(String ticker, String evaluationAmount, String weight) {
        return new HoldingResult(
                ticker,
                ticker + " name",
                money("1"),
                money("10000"),
                money(evaluationAmount),
                money(evaluationAmount),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                money(weight)
        );
    }

    private <T> ExternalApiResponseDto<T> success(T data) {
        return new ExternalApiResponseDto<>(200, "SUCCESS", data, null);
    }

    private Page<Holding> page(List<Holding> content, int pageNumber, long totalElements) {
        return new PageImpl<>(
                content,
                PageRequest.of(pageNumber, DEFAULT_SIZE),
                totalElements
        );
    }

    private Pageable pageNumber(int pageNumber) {
        return argThat(pageable ->
                pageable != null
                        && pageable.getPageNumber() == pageNumber
                        && pageable.getPageSize() == DEFAULT_SIZE
                        && pageable.getSort().getOrderFor("createdAt") != null
                        && pageable.getSort().getOrderFor("createdAt").isDescending()
        );
    }
}
