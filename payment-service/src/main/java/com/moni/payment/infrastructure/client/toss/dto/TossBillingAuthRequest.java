package com.moni.payment.infrastructure.client.toss.dto;

public record TossBillingAuthRequest(
        String authKey,
        String customerKey) {
}
