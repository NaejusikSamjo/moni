package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.CustomException;

public class ExternalServiceException extends CustomException {
    public ExternalServiceException() {
        super(PortfolioErrorCode.EXTERNAL_SERVICE_ERROR);
    }
}
