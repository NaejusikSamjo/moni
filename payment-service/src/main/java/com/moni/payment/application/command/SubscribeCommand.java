package com.moni.payment.application.command;

import java.util.UUID;

public record SubscribeCommand(
        UUID userId,
        String authKey,
        String customerKey,
        long amount,
        String requestedBy) {
}
