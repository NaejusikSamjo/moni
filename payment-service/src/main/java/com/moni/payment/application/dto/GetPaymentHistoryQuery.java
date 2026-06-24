package com.moni.payment.application.dto;

import java.util.UUID;

public record GetPaymentHistoryQuery(UUID userId, int page, int size) {
}
