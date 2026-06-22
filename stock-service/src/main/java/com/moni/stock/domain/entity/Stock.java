package com.moni.stock.domain.entity;

import com.moni.stock.domain.type.MarketType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class Stock {

    private UUID id;
    private String ticker;
    private String name;
    private MarketType market;
}