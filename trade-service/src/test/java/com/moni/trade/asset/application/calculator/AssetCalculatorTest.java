package com.moni.trade.asset.application.calculator;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.asset.application.calculator.model.AccountInput;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.application.calculator.model.TradeInput;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import com.moni.trade.trade.domain.enums.TradeStatus;
import com.moni.trade.trade.domain.enums.TradeType;
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
            AccountInput account = new AccountInput(money("60000"), money("100000"));
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("999991", 3L, money("10000"), money("30000")),
                    new HoldingInput("999992", 2L, money("15000"), money("30000"))
            );
            List<PriceInput> prices = List.of(
                    new PriceInput("999991", money("11000")),
                    new PriceInput("999992", money("13500"))
            );

            // when
            AssetResult result = assetCalculator.calculateAssets(account, holdings, prices);

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("120000.00");
            assertThat(result.cashBalance()).isEqualByComparingTo("60000");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("60000.00");
            assertThat(result.principalAmount()).isEqualByComparingTo("100000");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("20000.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("20.0000");
            assertThat(result.holdings()).extracting(HoldingResult::ticker)
                    .containsExactly("999991", "999992");
            assertThat(result.holdings().getFirst().evaluationAmount()).isEqualByComparingTo("33000.00");
            assertThat(result.holdings().getFirst().profitLoss()).isEqualByComparingTo("3000.00");
            assertThat(result.holdings().getFirst().profitRate()).isEqualByComparingTo("10.0000");
            assertThat(result.holdings().getFirst().weight()).isEqualByComparingTo("55.0000");
            assertThat(result.holdings().get(1).evaluationAmount()).isEqualByComparingTo("27000.00");
            assertThat(result.holdings().get(1).profitLoss()).isEqualByComparingTo("-3000.00");
            assertThat(result.holdings().get(1).profitRate()).isEqualByComparingTo("-10.0000");
            assertThat(result.holdings().get(1).weight()).isEqualByComparingTo("45.0000");
        }

        @Test
        @DisplayName("성공 - 보유 종목이 없으면 주식 평가금액과 수익률은 0이다")
        void success_empty_holdings() {
            // given
            AccountInput account = new AccountInput(money("10000"), money("10000"));

            // when
            AssetResult result = assetCalculator.calculateAssets(account, List.of(), List.of());

            // then
            assertThat(result.totalAsset()).isEqualByComparingTo("10000.00");
            assertThat(result.stockEvaluationAmount()).isEqualByComparingTo("0.00");
            assertThat(result.totalProfitLoss()).isEqualByComparingTo("0.00");
            assertThat(result.totalReturnRate()).isEqualByComparingTo("0.0000");
            assertThat(result.holdings()).isEmpty();
        }

        @Test
        @DisplayName("실패 - 현재가가 없는 종목이면 예외가 발생한다")
        void fail_stock_price_not_found() {
            // given
            AccountInput account = new AccountInput(money("10000"), money("10000"));
            List<HoldingInput> holdings = List.of(
                    new HoldingInput("999991", 1L, money("10000"), money("10000"))
            );

            // when & then
            assertThatThrownBy(() -> assetCalculator.calculateAssets(account, holdings, List.of()))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.STOCK_PRICE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("calculateCashBalance()")
    class CalculateCashBalance {

        @Test
        @DisplayName("성공 - 완료된 매수는 차감하고 매도는 가산한다")
        void success_calculate_cash_balance() {
            // given
            List<TradeInput> trades = List.of(
                    new TradeInput(TradeType.BUY, money("590000"), TradeStatus.DONE),
                    new TradeInput(TradeType.SELL, money("3585000"), TradeStatus.DONE),
                    new TradeInput(TradeType.BUY, money("100000"), TradeStatus.PENDING)
            );

            // when
            BigDecimal result = assetCalculator.calculateCashBalance(money("10000000"), trades);

            // then
            assertThat(result).isEqualByComparingTo("12995000");
        }

        @Test
        @DisplayName("실패 - 거래 응답 필수값이 없으면 예외가 발생한다")
        void fail_invalid_trade_response() {
            // given
            List<TradeInput> trades = List.of(
                    new TradeInput(null, money("10000"), TradeStatus.DONE)
            );

            // when & then
            assertThatThrownBy(() -> assetCalculator.calculateCashBalance(money("10000000"), trades))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(AssetErrorCode.TRADE_RESPONSE_INVALID));
        }
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
