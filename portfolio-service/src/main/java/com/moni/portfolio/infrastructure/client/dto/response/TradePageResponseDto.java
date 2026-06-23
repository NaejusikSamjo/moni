package com.moni.portfolio.infrastructure.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record TradePageResponseDto<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        @JsonAlias("isLast") boolean last
) {
}
