package com.moni.payment.common.exception;

import com.moni.common.error.exception.CustomException;

public class PaymentException extends CustomException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }
}
