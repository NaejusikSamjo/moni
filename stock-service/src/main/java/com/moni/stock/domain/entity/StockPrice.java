package com.moni.stock.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// Redis에 저장되는 실시간 가격 값 객체 (DB 저장 X)
@Getter
@Builder
public class StockPrice {

    private String ticker;
    private BigDecimal askPrice;
    private BigDecimal bidPrice;
    private BigDecimal currentPrice;
    private Long volume;
    private String section;
}