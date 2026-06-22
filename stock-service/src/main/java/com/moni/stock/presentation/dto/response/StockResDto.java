package com.moni.stock.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class StockResDto {

    public String ticker;

    public String name;

    public BigDecimal price;
}
