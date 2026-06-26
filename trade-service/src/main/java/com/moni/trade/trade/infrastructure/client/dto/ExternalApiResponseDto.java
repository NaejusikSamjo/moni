package com.moni.trade.trade.infrastructure.client.dto;

public record ExternalApiResponseDto<T>(
        int status,
        String message,
        T data,
        Object errors
) {
}
