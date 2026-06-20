package com.moni.payment.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moni.common.error.ErrorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message("SUCCESS")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(HttpStatus httpStatus, T data) {
        return ApiResponse.<T>builder()
                .status(httpStatus.value())
                .message("SUCCESS")
                .data(data)
                .build();
    }

    public static ApiResponse<Void> failure(HttpStatus httpStatus, String errorCode, String message) {
        return ApiResponse.<Void>builder()
                .status(httpStatus.value())
                .message(message)
                .error(ErrorResponse.of(errorCode, message))
                .build();
    }
}
