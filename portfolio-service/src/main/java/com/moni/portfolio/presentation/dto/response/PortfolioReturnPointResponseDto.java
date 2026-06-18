package com.moni.portfolio.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "포트폴리오 일별 수익률 응답")
public record PortfolioReturnPointResponseDto(

        @Schema(description = "기준일", example = "2026-06-18")
        LocalDate date,

        @Schema(description = "일간 수익률", example = "1.2345")
        BigDecimal dailyReturnRate,

        @Schema(description = "기준일 평가금액", example = "100000.00")
        BigDecimal evaluationAmount
) {
}
