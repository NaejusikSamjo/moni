package com.moni.portfolio.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "보유 종목 현황 조회 응답")
public record PortfolioHoldingsResponseDto(

        @Schema(description = "보유 종목 목록")
        List<PortfolioHoldingResponseDto> content,

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "10")
        int size,

        @Schema(description = "전체 요소 수", example = "2")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "정렬 조건", example = "evaluationAmount,desc")
        String sort
) {
}
