package com.moni.payment.application.dto.commandDto;

import java.time.LocalDate;
import java.util.UUID;

public record SubscribeResult(
        UUID paymentId,
        String status,
        long amount,
        LocalDate nextBillingDate) {
}
