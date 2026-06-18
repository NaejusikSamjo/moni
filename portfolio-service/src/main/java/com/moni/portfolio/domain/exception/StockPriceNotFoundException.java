package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.CustomException;

public class StockPriceNotFoundException extends CustomException {
    public StockPriceNotFoundException() {
        super(PortfolioErrorCode.STOCK_PRICE_NOT_FOUND);
    }
}
