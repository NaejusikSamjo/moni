package com.moni.portfolio.infrastructure.client.dto.response;

import java.util.List;

public record TradeAssetHoldingsResponseDto(
        List<TradeAssetHoldingResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {
}
