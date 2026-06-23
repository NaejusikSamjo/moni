package com.moni.payment.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record SubscribeResult(
        UUID paymentId,
        String status,
        long amount,
        LocalDate nextBillingDate) {
}
