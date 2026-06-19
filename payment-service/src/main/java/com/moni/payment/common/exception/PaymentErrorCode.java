package com.moni.payment.common.exception;

import com.moni.common.error.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PaymentErrorCode implements ErrorCode {

    INVALID_PAYMENT_STATUS_TRANSITION("PAY_001", "허용되지 않는 결제 상태 전이입니다.", HttpStatus.BAD_REQUEST),
    INVALID_MERCHANT_ID_FORMAT("PAY_002", "merchantId는 영문/숫자/-/_로 구성된 6~64자여야 합니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND("PAY_003", "결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_PAYMENT("PAY_004", "중복 결제 요청입니다.", HttpStatus.CONFLICT),
    NEGATIVE_AMOUNT("PAY_005", "결제 금액은 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),

    INVALID_SUBSCRIPTION_STATUS_TRANSITION("SUB_001", "허용되지 않는 구독 상태 전이입니다.", HttpStatus.BAD_REQUEST),
    SUBSCRIPTION_NOT_FOUND("SUB_002", "구독 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACTIVE_SUBSCRIPTION_EXISTS("SUB_003", "이미 활성화된 구독이 존재합니다.", HttpStatus.CONFLICT),
    INVALID_BILLING_KEY("SUB_004", "유효하지 않은 BillingKey입니다.", HttpStatus.BAD_REQUEST),

    PG_COMMUNICATION_ERROR("PG_001", "PG사와 통신 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    PG_PAYMENT_FAILED("PG_002", "PG사에서 결제를 거절하였습니다.", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final String message;
    private final HttpStatus status;

    PaymentErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}
