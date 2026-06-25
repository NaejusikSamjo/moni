package com.moni.portfolio.application.calculator;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.calculator.model.AccountInput;
import com.moni.portfolio.application.calculator.model.HoldingInput;
import com.moni.portfolio.application.calculator.model.HoldingResult;
import com.moni.portfolio.application.calculator.model.PortfolioAssetResult;
import com.moni.portfolio.application.calculator.model.PriceInput;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PortfolioCalculator 테스트")
class PortfolioCalculatorTest {

    private final PortfolioCalculator portfolioCalculator = new PortfolioCalculator();

    @Nested
    @DisplayName("calculateAssets()")
    class CalculateAssets {

        @Test
        @DisplayName("성공 - 보유 종목별 평가값과 전체 자산을 계산한다")
        void success() {
            // given
            AccountInput account = new AccountInput(money("40000"), money("90000"));
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 3L, money("10000"), money("30000")),
                    new HoldingInput("TICKER-2", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", money("20000")),
                    new PriceInput("TICKER-2", money("10000"))
            );

            // when
            PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("110000.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("30000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("80000.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("90000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("20000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("22.2222");
            assertThat(result.holdings()).hasSize(2);

            HoldingResult firstHolding = result.holdings().get(0);
            assertThat(firstHolding.ticker()).isEqualTo("TICKER-1");
            assertThat(firstHolding.evaluationAmount()).isEqualByComparingTo("60000.00");
            assertThat(firstHolding.profitLoss()).isEqualByComparingTo("30000.00");
            assertThat(firstHolding.profitRate()).isEqualByComparingTo("100.0000");
            assertThat(firstHolding.weight()).isEqualByComparingTo("75.0000");

            HoldingResult secondHolding = result.holdings().get(1);
            assertThat(secondHolding.ticker()).isEqualTo("TICKER-2");
            assertThat(secondHolding.evaluationAmount()).isEqualByComparingTo("20000.00");
            assertThat(secondHolding.profitLoss()).isEqualByComparingTo("-10000.00");
            assertThat(secondHolding.profitRate()).isEqualByComparingTo("-33.3333");
            assertThat(secondHolding.weight()).isEqualByComparingTo("25.0000");
        }

        @Test
        @DisplayName("성공 - 계좌 잔액 대신 고정 원금에서 누적 매수 금액을 차감해 예수금을 계산한다")
        void success_calculate_cash_balance_from_principal_and_purchase_amount() {
            // given
            AccountInput account = new AccountInput(money("10000000"), money("10000000"));
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("005930", 10L, money("59000"), money("590000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("005930", money("358500"))
            );

            // when
            PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

            // then
            assertThat(result.cashBalance()).isEqualByComparingTo("9410000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("3585000.00");
            assertThat(result.totalAsset()).isEqualByComparingTo("12995000.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("2995000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("29.9500");
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 주식 평가금액과 수익률은 0으로 계산한다")
        void success_empty_holdings() {
            // given
            AccountInput account = new AccountInput(money("10000"), money("10000"));
            List<HoldingInput> holdings = List.of();
            List<PriceInput> prices = List.of();

            // when
            PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("0.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("0.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("0.0000");
            assertThat(result.holdings()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 투자 원금이 0이면 누적 수익률은 0으로 계산한다")
        void success_zero_principal_amount() {
            // given
            AccountInput account = new AccountInput(money("10000"), BigDecimal.ZERO);
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 10L, BigDecimal.ZERO, BigDecimal.ZERO)
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", money("1000"))
            );

            // when
            PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("10000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("0.0000");
        }

        @Test
        @DisplayName("성공 - 매수 금액이 0이면 종목 수익률은 0으로 계산한다")
        void success_zero_purchase_amount() {
            // given
            AccountInput account = new AccountInput(BigDecimal.ZERO, BigDecimal.ZERO);
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 10L, BigDecimal.ZERO, BigDecimal.ZERO)
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", money("100"))
            );

            // when
            PortfolioAssetResult result = portfolioCalculator.calculateAssets(account, holdings, prices);

            // then
            HoldingResult holding = result.holdings().get(0);
            assertThat(holding.evaluationAmount()).isEqualByComparingTo("1000.00");
            assertThat(holding.profitLoss()).isEqualByComparingTo("1000.00");
            assertThat(holding.profitRate()).isEqualByComparingTo("0.0000");
            assertThat(holding.weight()).isEqualByComparingTo("100.0000");
        }

        @Test
        @DisplayName("실패 - 현재가 정보가 없으면 예외가 발생한다")
        void fail_stock_price_not_found() {
            // given
            AccountInput account = new AccountInput(money("10000"), money("10000"));
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 1L, money("10000"), money("10000"))
            );
            List<PriceInput> prices = List.of();

            // when & then
            assertThatThrownBy(() -> portfolioCalculator.calculateAssets(account, holdings, prices))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("calculateHoldings()")
    class CalculateHoldings {

        @Test
        @DisplayName("성공 - 보유 종목별 평가값과 전체 평가금액 기준 비중을 계산한다")
        void success() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 3L, money("10000"), money("30000")),
                    new HoldingInput("TICKER-2", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", money("20000")),
                    new PriceInput("TICKER-2", money("10000"))
            );

            // when
            List<HoldingResult> result = portfolioCalculator.calculateHoldings(holdings, prices);

            // then
            assertThat(result).hasSize(2);

            HoldingResult firstHolding = result.get(0);
            assertThat(firstHolding.ticker()).isEqualTo("TICKER-1");
            assertThat(firstHolding.evaluationAmount()).isEqualByComparingTo("60000.00");
            assertThat(firstHolding.profitLoss()).isEqualByComparingTo("30000.00");
            assertThat(firstHolding.profitRate()).isEqualByComparingTo("100.0000");
            assertThat(firstHolding.weight()).isEqualByComparingTo("75.0000");

            HoldingResult secondHolding = result.get(1);
            assertThat(secondHolding.ticker()).isEqualTo("TICKER-2");
            assertThat(secondHolding.evaluationAmount()).isEqualByComparingTo("20000.00");
            assertThat(secondHolding.profitLoss()).isEqualByComparingTo("-10000.00");
            assertThat(secondHolding.profitRate()).isEqualByComparingTo("-33.3333");
            assertThat(secondHolding.weight()).isEqualByComparingTo("25.0000");
        }

        @Test
        @DisplayName("성공 - 반올림된 평균 매수가 대신 누적 매수 금액으로 손익을 계산한다")
        void success_calculate_with_total_purchase_amount() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 3L, money("10.01"), money("30.02"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", money("11.00"))
            );

            // when
            List<HoldingResult> result = portfolioCalculator.calculateHoldings(holdings, prices);

            // then
            HoldingResult holding = result.getFirst();
            assertThat(holding.evaluationAmount()).isEqualByComparingTo("33.00");
            assertThat(holding.profitLoss()).isEqualByComparingTo("2.98");
            assertThat(holding.profitRate()).isEqualByComparingTo("9.9267");
        }

        @Test
        @DisplayName("성공 - 모든 종목의 평가금액이 0이면 비중을 0으로 계산한다")
        void success_zero_evaluation_amount() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 1L, money("10000"), money("10000")),
                    new HoldingInput("TICKER-2", 2L, money("5000"), money("10000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("TICKER-1", BigDecimal.ZERO),
                    new PriceInput("TICKER-2", BigDecimal.ZERO)
            );

            // when
            List<HoldingResult> result = portfolioCalculator.calculateHoldings(holdings, prices);

            // then
            assertThat(result).allSatisfy(holding -> {
                assertThat(holding.evaluationAmount()).isEqualByComparingTo("0.00");
                assertThat(holding.weight()).isEqualByComparingTo("0.0000");
            });
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 빈 목록을 반환한다")
        void success_empty_holdings() {
            // given
            List<HoldingInput> holdings = List.of();
            List<PriceInput> prices = List.of();

            // when
            List<HoldingResult> result = portfolioCalculator.calculateHoldings(holdings, prices);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패 - 보유 종목의 현재가가 없으면 예외가 발생한다")
        void fail_stock_price_not_found() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("TICKER-1", 1L, money("10000"), money("10000"))
            );
            List<PriceInput> prices = List.of();

            // when & then
            assertThatThrownBy(() -> portfolioCalculator.calculateHoldings(holdings, prices))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND));
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
