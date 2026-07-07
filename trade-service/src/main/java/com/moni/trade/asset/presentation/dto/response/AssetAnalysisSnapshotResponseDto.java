package com.moni.trade.asset.presentation.dto.response;

import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "AI 분석용 자산 스냅샷 응답")
public record AssetAnalysisSnapshotResponseDto(

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
        BigDecimal totalReturnRate,

        @Schema(description = "현재 보유 종목 평가손익 합계", example = "203500.00")
        BigDecimal stockProfitLoss,

        @Schema(description = "현재 보유 종목 기준 수익률(%)", example = "3.2670")
        BigDecimal stockReturnRate,

        @Schema(description = "보유 비중 상위 보유 종목 목록")
        List<AssetHoldingResponseDto> holdings
) {
    public static AssetAnalysisSnapshotResponseDto from(
            AssetResult assetResult,
            List<HoldingResult> holdingResults
    ) {
        return new AssetAnalysisSnapshotResponseDto(
                assetResult.totalAsset(),
                assetResult.cashBalance(),
                assetResult.stockEvaluationAmount(),
                assetResult.principalAmount(),
                assetResult.totalProfitLoss(),
                assetResult.totalReturnRate(),
                assetResult.stockProfitLoss(),
                assetResult.stockReturnRate(),
                holdingResults.stream()
                        .map(AssetHoldingResponseDto::from)
                        .toList()
        );
    }
}
