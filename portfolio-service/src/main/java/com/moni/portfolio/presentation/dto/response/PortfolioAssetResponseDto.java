package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.application.calculator.model.PortfolioAssetResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "포트폴리오 자산 조회 응답")
public record PortfolioAssetResponseDto(

        @Schema(description = "총 평가자산", example = "100000.00")
        BigDecimal totalAsset,

        @Schema(description = "예수금", example = "40000.00")
        BigDecimal cashBalance,

        @Schema(description = "주식 평가금액", example = "60000.00")
        BigDecimal stockEvaluationAmount,

        @Schema(description = "투자 원금", example = "90000.00")
        BigDecimal principalAmount,

        @Schema(description = "총 손익", example = "10000.00")
        BigDecimal totalProfitLoss,

        @Schema(description = "누적 수익률", example = "11.1111")
        BigDecimal totalReturnRate
) {
    public static PortfolioAssetResponseDto from(PortfolioAssetResult result) {
        return new PortfolioAssetResponseDto(
                result.totalAsset(),
                result.cashBalance(),
                result.stockEvaluationAmount(),
                result.principalAmount(),
                result.totalProfitLoss(),
                result.totalReturnRate()
        );
    }
}
