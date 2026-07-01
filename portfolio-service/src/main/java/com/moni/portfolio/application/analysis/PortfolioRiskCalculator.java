package com.moni.portfolio.application.analysis;

import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PortfolioRiskCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TOP_HOLDING_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal TOP_SECTOR_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal STOCK_ALLOCATION_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal HOLDING_COUNT_WEIGHT = new BigDecimal("0.15");

    public TendencySuitabilityResult calculate(
            UserTendencyResponseDto userTendency,
            BigDecimal totalAsset,
            BigDecimal stockEvaluationAmount,
            BigDecimal topHoldingWeight,
            BigDecimal topSectorWeight,
            int holdingCount
    ) {
        int userTendencyScore = userTendency.score();
        TendencyType userTendencyType = TendencyType.fromName(userTendency.type());
        int portfolioRiskScore = calculatePortfolioRiskScore(
                totalAsset,
                stockEvaluationAmount,
                topHoldingWeight,
                topSectorWeight,
                holdingCount
        );
        TendencyType portfolioRiskType = TendencyType.fromScore(portfolioRiskScore);
        int suitabilityScore = Math.max(0, HUNDRED.intValue() - Math.abs(userTendencyScore - portfolioRiskScore));

        return new TendencySuitabilityResult(
                userTendencyType,
                userTendencyScore,
                portfolioRiskType,
                portfolioRiskScore,
                suitabilityScore,
                resolveSuitabilityLevel(suitabilityScore)
        );
    }

    private int calculatePortfolioRiskScore(
            BigDecimal totalAsset,
            BigDecimal stockEvaluationAmount,
            BigDecimal topHoldingWeight,
            BigDecimal topSectorWeight,
            int holdingCount
    ) {
        BigDecimal stockAllocationRate = calculateStockAllocationRate(totalAsset, stockEvaluationAmount);
        BigDecimal holdingCountRisk = BigDecimal.valueOf(resolveHoldingCountRisk(holdingCount));

        BigDecimal score = normalizePercent(topHoldingWeight).multiply(TOP_HOLDING_WEIGHT)
                .add(normalizePercent(topSectorWeight).multiply(TOP_SECTOR_WEIGHT))
                .add(stockAllocationRate.multiply(STOCK_ALLOCATION_WEIGHT))
                .add(holdingCountRisk.multiply(HOLDING_COUNT_WEIGHT));

        return score.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal calculateStockAllocationRate(BigDecimal totalAsset, BigDecimal stockEvaluationAmount) {
        if (totalAsset == null
                || totalAsset.compareTo(BigDecimal.ZERO) == 0
                || stockEvaluationAmount == null) {
            return BigDecimal.ZERO;
        }

        return stockEvaluationAmount
                .multiply(HUNDRED)
                .divide(totalAsset, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return value;
    }

    private int resolveHoldingCountRisk(int holdingCount) {
        if (holdingCount <= 1) {
            return 100;
        }
        if (holdingCount <= 2) {
            return 80;
        }
        if (holdingCount <= 3) {
            return 60;
        }
        if (holdingCount <= 5) {
            return 40;
        }
        if (holdingCount <= 8) {
            return 20;
        }
        return 0;
    }

    private String resolveSuitabilityLevel(int suitabilityScore) {
        if (suitabilityScore >= 80) {
            return "적합";
        }
        if (suitabilityScore >= 60) {
            return "보통";
        }
        if (suitabilityScore >= 40) {
            return "주의";
        }
        return "위험";
    }
}
