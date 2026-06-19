package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.domain.entity.Portfolio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "포트폴리오 생성 응답")
public record PortfolioCreateResponseDto(

        @Schema(description = "포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "사용자 ID")
        UUID userId
) {
    public static PortfolioCreateResponseDto from(Portfolio portfolio) {
        return new PortfolioCreateResponseDto(
                portfolio.getId(),
                portfolio.getUserId()
        );
    }
}
