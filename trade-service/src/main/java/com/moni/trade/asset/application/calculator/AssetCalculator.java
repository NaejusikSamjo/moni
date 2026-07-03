package com.moni.trade.asset.application.calculator;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AssetCalculator {

    private static final int MONEY_SCALE = 2; // 금액은 소수점 둘째 자리까지 반올림
    private static final int RATE_SCALE = 4; // 수익률과 비중은 퍼센트 기준 소수점 넷째 자리까지 반올림
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    /** 보유 종목별 평가 결과를 계산한 뒤 전체 자산 요약 결과 반환 */
    public AssetResult calculateAssets(
            BigDecimal cashBalance,
            BigDecimal principalAmount,
            List<HoldingInput> holdings,
            List<PriceInput> prices
    ) {
        List<HoldingResult> holdingResults = calculateHoldingResults(holdings, prices);

        // 주식 평가금액은 모든 보유 종목 평가금액의 합계
        BigDecimal stockEvaluationAmount = sumEvaluationAmount(holdingResults);

        // 총 평가자산과 전체 손익은 계좌 예수금, 주식 평가금액, 투자 원금을 기준으로 계산
        BigDecimal totalAsset = cashBalance.add(stockEvaluationAmount);
        BigDecimal totalProfitLoss = totalAsset.subtract(principalAmount);
        BigDecimal totalReturnRate = calculateRate(totalProfitLoss, principalAmount);

        return new AssetResult(
                totalAsset.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                cashBalance,
                stockEvaluationAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                principalAmount,
                totalProfitLoss.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                totalReturnRate
        );
    }

    /** 계좌 정보 없이 보유 종목별 평가금액, 평가손익, 수익률, 비중을 계산 */
    public List<HoldingResult> calculateHoldings(
            List<HoldingInput> holdings,
            List<PriceInput> prices
    ) {
        List<HoldingResult> holdingResults = calculateHoldingResults(holdings, prices);

        BigDecimal stockEvaluationAmount = sumEvaluationAmount(holdingResults);

        return holdingResults.stream()
                .map(result -> result.withWeight(calculateRate(result.evaluationAmount(), stockEvaluationAmount)))
                .toList();
    }

    private List<HoldingResult> calculateHoldingResults(
            List<HoldingInput> holdings,
            List<PriceInput> prices
    ) {
        // 현재가는 ticker 기준으로 빠르게 찾을 수 있도록 Map으로 변환
        Map<String, PriceInput> priceMap = prices.stream()
                .collect(Collectors.toMap(PriceInput::ticker, Function.identity()));

        return holdings.stream()
                .map(holding -> calculateHolding(holding, priceMap))
                .toList();
    }

    private BigDecimal sumEvaluationAmount(List<HoldingResult> holdingResults) {
        return holdingResults.stream()
                .map(HoldingResult::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 단일 보유 종목의 평가금액, 평가손익, 수익률을 계산 */
    private HoldingResult calculateHolding(HoldingInput holding, Map<String, PriceInput> priceMap) {
        PriceInput price = priceMap.get(holding.ticker());

        if (price == null) {
            throw new CustomException(AssetErrorCode.STOCK_PRICE_NOT_FOUND);
        }

        BigDecimal quantity = BigDecimal.valueOf(holding.quantity());
        BigDecimal purchaseAmount = holding.totalPurchaseAmount();
        BigDecimal evaluationAmount = price.currentPrice().multiply(quantity);
        BigDecimal profitLoss = evaluationAmount.subtract(purchaseAmount);
        BigDecimal profitRate = calculateRate(profitLoss, purchaseAmount);

        return new HoldingResult(
                holding.ticker(),
                price.name(),
                holding.quantity(),
                holding.averagePurchasePrice(),
                price.currentPrice(),
                evaluationAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                profitLoss.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                profitRate,
                BigDecimal.ZERO
        );
    }

    /** numerator / denominator * 100 형태의 퍼센트 값을 계산 */
    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }

        return numerator
                .multiply(HUNDRED)
                .divide(denominator, RATE_SCALE, RoundingMode.HALF_UP);
    }
}
