package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.CustomException;

public class PortfolioNotFoundException extends CustomException {
    public PortfolioNotFoundException() {
        super(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }
}
