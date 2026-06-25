package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.application.calculator.model.HoldingResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "보유 종목 평가 응답")
public record PortfolioHoldingResponseDto(

        @Schema(description = "종목 코드", example = "005930")
        String ticker,

        @Schema(description = "보유 수량", example = "10")
        Long quantity,

        @Schema(description = "평균 매수 단가", example = "70000.00")
        BigDecimal averagePurchasePrice,

        @Schema(description = "현재가", example = "75000.00")
        BigDecimal currentPrice,

        @Schema(description = "평가금액", example = "750000.00")
        BigDecimal evaluationAmount,

        @Schema(description = "평가손익", example = "50000.00")
        BigDecimal profitLoss,

        @Schema(description = "수익률(%)", example = "7.1429")
        BigDecimal profitRate,

        @Schema(description = "포트폴리오 내 비중(%)", example = "60.0000")
        BigDecimal weight
) {
    public static PortfolioHoldingResponseDto from(HoldingResult result) {
        return new PortfolioHoldingResponseDto(
                result.ticker(),
                result.quantity(),
                result.averagePurchasePrice(),
                result.currentPrice(),
                result.evaluationAmount(),
                result.profitLoss(),
                result.profitRate(),
                result.weight()
        );
    }
}
