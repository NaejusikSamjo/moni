package com.moni.portfolio.infrastructure.client.dto.response;

import java.util.UUID;

public record UserTendencyResponseDto(
        UUID id,
        Integer score,
        String type
) {
}
