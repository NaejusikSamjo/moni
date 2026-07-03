package com.moni.trade.trade.infrastructure.client;

import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** local 프로파일 전용 - stock-service 없이 고정 가격을 반환한다. */
@Primary
@Profile("local")
@Component
public class LocalStockServiceClientMock implements StockServiceClient {

    private static final Map<String, BigDecimal> MOCK_PRICES = Map.of(
            "005930", new BigDecimal("85000"),   // 삼성전자
            "000660", new BigDecimal("180000"),  // SK하이닉스
            "035420", new BigDecimal("55000"),   // NAVER
            "051910", new BigDecimal("350000"),  // LG화학
            "207940", new BigDecimal("700000")   // 삼성바이오로직스
    );

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("50000");

    @Override
    public ExternalApiResponseDto<StockPriceResponseDto> getStock(String ticker) {
        BigDecimal price = MOCK_PRICES.getOrDefault(ticker, DEFAULT_PRICE);
        return new ExternalApiResponseDto<>(200, "OK",
                new StockPriceResponseDto(ticker, "Mock Stock", price), null);
    }
}
