package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.CustomException;

public class PortfolioAlreadyExistsException extends CustomException {
    public PortfolioAlreadyExistsException() {
        super(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
    }
}
