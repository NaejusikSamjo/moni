package com.moni.payment.payment.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
        @NotBlank String authKey,
        @NotBlank String customerKey,
        @Min(1) long amount,
        @NotBlank String orderName) {
}
