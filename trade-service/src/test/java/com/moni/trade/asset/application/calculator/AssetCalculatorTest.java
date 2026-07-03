package com.moni.trade.asset.application.calculator;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AssetCalculator 테스트")
class AssetCalculatorTest {

    private final AssetCalculator assetCalculator = new AssetCalculator();

    @Nested
    @DisplayName("calculateAssets()")
    class CalculateAssets {

        @Test
        @DisplayName("성공 - 보유 종목 평가 결과와 전체 자산을 계산한다")
        void success_calculate_assets() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("999991", 3L, money("10000"), money("30000")),
                    new HoldingInput("999992", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("999991", "첫 번째 종목", money("11000")),
                    new PriceInput("999992", "두 번째 종목", money("14500"))
            );

            // when
            AssetResult result = assetCalculator.calculateAssets(
                    money("60000"),
                    money("100000"),
                    holdings,
                    prices
            );

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("122000.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("60000");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("62000.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("100000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("22000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("22.0000");
            assertThat(result.stockProfitLoss()).isEqualByComparingTo("2000.00");
            assertThat(result.stockReturnRate()).isEqualByComparingTo("3.3333");
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 주식 평가금액과 수익률은 0이다")
        void success_empty_holdings() {
            // when
            AssetResult result = assetCalculator.calculateAssets(
                    money("10000"),
                    money("10000"),
                    List.of(),
                    List.of()
            );

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("0.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("0.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("0.0000");
            assertThat(result.stockProfitLoss()).isEqualByComparingTo("0.00");
            assertThat(result.stockReturnRate()).isEqualByComparingTo("0.0000");
        }

        @Test
        @DisplayName("실패 - 현재가가 없는 종목이면 예외가 발생한다")
        void fail_stock_price_not_found() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("999991", 1L, money("10000"), money("10000"))
            );

            // when & then
            assertThatThrownBy(() -> assetCalculator.calculateAssets(
                    money("10000"),
                    money("10000"),
                    holdings,
                    List.of()
            ))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("calculateHoldings()")
    class CalculateHoldings {

        @Test
        @DisplayName("성공 - 보유 종목별 평가 결과와 비중을 계산한다")
        void success_calculate_holdings() {
            // given
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("999991", 3L, money("10000"), money("30000")),
                    new HoldingInput("999992", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("999991", "첫 번째 종목", money("11000")),
                    new PriceInput("999992", "두 번째 종목", money("13500"))
            );

            // when
            List<HoldingResult> result = assetCalculator.calculateHoldings(holdings, prices);

            // then
            assertThat(result).extracting(HoldingResult::ticker)
                    .containsExactly("999991", "999992");
            assertThat(result).extracting(HoldingResult::name)
                    .containsExactly("첫 번째 종목", "두 번째 종목");
            assertThat(result.getFirst().evaluationAmount()).isEqualByComparingTo("33000.00");
            assertThat(result.getFirst().profitLoss()).isEqualByComparingTo("3000.00");
            assertThat(result.getFirst().profitRate()).isEqualByComparingTo("10.0000");
            assertThat(result.getFirst().weight()).isEqualByComparingTo("55.0000");
            assertThat(result.get(1).evaluationAmount()).isEqualByComparingTo("27000.00");
            assertThat(result.get(1).profitLoss()).isEqualByComparingTo("-3000.00");
            assertThat(result.get(1).profitRate()).isEqualByComparingTo("-10.0000");
            assertThat(result.get(1).weight()).isEqualByComparingTo("45.0000");
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
