package com.moni.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moni.common.error.ErrorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {
    private int status;
    private String message;
    private T data;
    private ErrorResponse errors;

    public static <T> GlobalResponse<T> success(int status, T data) {
        return GlobalResponse.<T>builder()
                .status(status)
                .message("SUCCESS")
                .data(data)
                .build();
    }

    public static GlobalResponse<Void> failure(int status, String message, ErrorResponse errorResponse) {
        return GlobalResponse.<Void>builder()
                .status(status)
                .message(message)
                .errors(errorResponse)
                .build();
    }

}