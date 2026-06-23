package com.moni.payment.payment.adapter.out.pg.toss.dto;

public record TossBillingAuthRequest(
        String authKey,
        String customerKey) {
}
