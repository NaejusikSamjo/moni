package com.moni.payment.application.command;

import java.util.UUID;

public record CancelSubscriptionCommand(UUID subscriptionId, UUID userId, String reason) {
}
