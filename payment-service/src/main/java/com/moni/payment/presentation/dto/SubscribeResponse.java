package com.moni.payment.presentation.dto;

import com.moni.payment.application.dto.commandDto.SubscribeResult;

import java.time.LocalDate;
import java.util.UUID;

public record SubscribeResponse(
        UUID paymentId,
        String status,
        long amount,
        LocalDate nextBillingDate) {

    public static SubscribeResponse from(SubscribeResult result) {
        String apiStatus = "COMPLETED".equals(result.status()) ? "SUCCESS" : result.status();
        return new SubscribeResponse(
                result.paymentId(),
                apiStatus,
                result.amount(),
                result.nextBillingDate());
    }
}
