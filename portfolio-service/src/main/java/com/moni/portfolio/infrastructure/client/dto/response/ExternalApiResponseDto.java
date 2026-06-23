package com.moni.portfolio.infrastructure.client.dto.response;

public record ExternalApiResponseDto<T>(
        int status,
        String message,
        T data,
        Object errors
) {
}
