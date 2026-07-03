package com.moni.trade.reservedorder.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservedOrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("RESERVED_001", "예약 주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_CANNOT_BE_CANCELLED("RESERVED_002", "이미 처리된 주문은 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_REQUEST("RESERVED_003", "지정가 주문에는 목표 가격이 필요합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
