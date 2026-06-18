package com.moni.portfolio.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "종목별 손익 조회 응답")
public record PortfolioHoldingProfitLossResponseDto(

        @Schema(description = "종목 코드", example = "TICKER-1")
        String ticker,

        @Schema(description = "보유 수량", example = "10")
        Long quantity,

        @Schema(description = "평균 매수 단가", example = "10000.00")
        BigDecimal averagePurchasePrice,

        @Schema(description = "현재가", example = "12000.00")
        BigDecimal currentPrice,

        @Schema(description = "평가금액", example = "120000.00")
        BigDecimal evaluationAmount,

        @Schema(description = "평가손익", example = "20000.00")
        BigDecimal profitLoss,

        @Schema(description = "수익률", example = "20.0000")
        BigDecimal profitRate,

        @Schema(description = "실현손익", example = "5000.00")
        BigDecimal realizedProfitLoss
) {
}
