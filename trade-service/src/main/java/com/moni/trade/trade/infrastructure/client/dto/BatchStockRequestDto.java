package com.moni.trade.trade.infrastructure.client.dto;

import java.util.List;

public record BatchStockRequestDto(
        List<String> tickers
) {
}
