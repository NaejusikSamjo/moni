package com.moni.payment.payment.adapter.out.pg.toss.dto;

public record TossBillingChargeRequest(
        String customerKey,
        long amount,
        String orderId,
        String orderName) {
}
