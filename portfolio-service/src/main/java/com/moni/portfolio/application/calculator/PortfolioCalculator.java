package com.moni.portfolio.application.calculator;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.calculator.model.AccountInput;
import com.moni.portfolio.application.calculator.model.HoldingInput;
import com.moni.portfolio.application.calculator.model.HoldingResult;
import com.moni.portfolio.application.calculator.model.PortfolioAssetResult;
import com.moni.portfolio.application.calculator.model.PriceInput;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 외부 서비스에서 조회한 계좌, 보유 종목, 현재가 데이터를 기반으로 포트폴리오 평가 값을 계산 */
@Component
public class PortfolioCalculator {

    private static final int MONEY_SCALE = 2; // 금액은 소수점 둘째 자리까지 반올림
    private static final int RATE_SCALE = 4; // 수익률과 비중은 퍼센트 기준 소수점 넷째 자리까지 반올림
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    /** 보유 종목별 평가 결과를 계산한 뒤 전체 자산 요약 결과 반환 */
    public PortfolioAssetResult calculateAssets(
            AccountInput account,
            List<HoldingInput> holdings,
            List<PriceInput> prices
    ) {
        List<HoldingResult> holdingResults = calculateHoldings(holdings, prices);

        // 주식 평가금액은 모든 보유 종목 평가금액의 합계
        BigDecimal stockEvaluationAmount = holdingResults.stream()
                .map(HoldingResult::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 평가자산과 전체 손익은 계좌 예수금, 주식 평가금액, 투자 원금을 기준으로 계산
        BigDecimal totalAsset = account.cashBalance().add(stockEvaluationAmount);
        BigDecimal totalProfitLoss = totalAsset.subtract(account.principalAmount());
        BigDecimal totalReturnRate = calculateRate(totalProfitLoss, account.principalAmount());

        return new PortfolioAssetResult(
                totalAsset.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                account.cashBalance(),
                stockEvaluationAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                account.principalAmount(),
                totalProfitLoss.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                totalReturnRate,
                holdingResults
        );
    }

    /** 계좌 정보 없이 보유 종목별 평가금액, 평가손익, 수익률, 비중을 계산 */
    public List<HoldingResult> calculateHoldings(
            List<HoldingInput> holdings,
            List<PriceInput> prices
    ) {
        // 현재가는 ticker 기준으로 빠르게 찾을 수 있도록 Map으로 변환
        Map<String, PriceInput> priceMap = prices.stream()
                .collect(Collectors.toMap(PriceInput::ticker, Function.identity()));

        List<HoldingResult> holdingResults = holdings.stream()
                .map(holding -> calculateHolding(holding, priceMap))
                .toList();

        BigDecimal stockEvaluationAmount = holdingResults.stream()
                .map(HoldingResult::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return holdingResults.stream()
                .map(result -> result.withWeight(calculateRate(result.evaluationAmount(), stockEvaluationAmount)))
                .toList();
    }

    /** 단일 보유 종목의 평가금액, 평가손익, 수익률을 계산 */
    private HoldingResult calculateHolding(HoldingInput holding, Map<String, PriceInput> priceMap) {
        PriceInput price = priceMap.get(holding.ticker());

        if (price == null) {
            throw new CustomException(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND);
        }

        BigDecimal quantity = BigDecimal.valueOf(holding.quantity());
        BigDecimal purchaseAmount = holding.averagePurchasePrice().multiply(quantity);
        BigDecimal evaluationAmount = price.currentPrice().multiply(quantity);
        BigDecimal profitLoss = evaluationAmount.subtract(purchaseAmount);
        BigDecimal profitRate = calculateRate(profitLoss, purchaseAmount);

        return new HoldingResult(
                holding.ticker(),
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
