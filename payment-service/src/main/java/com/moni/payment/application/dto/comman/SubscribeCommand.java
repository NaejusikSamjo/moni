package com.moni.payment.application.dto.comman;

import java.util.UUID;

public record SubscribeCommand(
        UUID userId,
        String authKey,
        String customerKey,
        long amount,
        String requestedBy) {
}
