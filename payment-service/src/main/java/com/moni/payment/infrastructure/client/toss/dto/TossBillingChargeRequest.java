package com.moni.payment.infrastructure.client.toss.dto;

public record TossBillingChargeRequest(
        String customerKey,
        long amount,
        String orderId,
        String orderName) {
}
