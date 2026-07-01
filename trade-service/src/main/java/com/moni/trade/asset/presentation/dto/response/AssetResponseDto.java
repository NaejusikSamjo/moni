package com.moni.trade.asset.presentation.dto.response;

import com.moni.trade.asset.application.calculator.model.AssetResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "포트폴리오 자산 조회 응답")
public record AssetResponseDto(

        @Schema(description = "총 평가자산", example = "10500000.00")
        BigDecimal totalAsset,

        @Schema(description = "예수금", example = "4000000.00")
        BigDecimal cashBalance,

        @Schema(description = "주식 평가금액", example = "6500000.00")
        BigDecimal stockEvaluationAmount,

        @Schema(description = "투자 원금", example = "10000000.00")
        BigDecimal principalAmount,

        @Schema(description = "총 손익", example = "500000.00")
        BigDecimal totalProfitLoss,

        @Schema(description = "누적 수익률(%)", example = "5.0000")
        BigDecimal totalReturnRate
) {
    public static AssetResponseDto from(AssetResult result) {
        return new AssetResponseDto(
                result.totalAsset(),
                result.cashBalance(),
                result.stockEvaluationAmount(),
                result.principalAmount(),
                result.totalProfitLoss(),
                result.totalReturnRate()
        );
    }
}
