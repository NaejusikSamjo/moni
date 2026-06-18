package com.moni.portfolio.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "포트폴리오 수익률 조회 응답")
public record PortfolioReturnsResponseDto(

        @Schema(description = "누적 수익률", example = "11.1111")
        BigDecimal totalReturnRate,

        @Schema(description = "일별 수익률 목록")
        List<PortfolioReturnPointResponseDto> returns
) {
}
