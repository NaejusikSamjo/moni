package com.moni.payment.infrastructure.client.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
        String paymentKey,
        String billingKey,
        String status,
        Long totalAmount,
        String method,
        String orderId,
        String orderName,
        String code,
        String message) {
}
