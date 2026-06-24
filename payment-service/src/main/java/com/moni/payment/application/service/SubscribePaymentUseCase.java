package com.moni.payment.application.service;

import com.moni.payment.application.dto.commandDto.ApprovePaymentCommand;
import com.moni.payment.application.dto.commandDto.FailPaymentCommand;
import com.moni.payment.application.dto.commandDto.RecordPendingPaymentCommand;
import com.moni.payment.application.dto.commandDto.SubscribeCommand;
import com.moni.payment.application.dto.commandDto.SubscribeResult;
import com.moni.payment.application.service.CommandService.PaymentCommandService;
import com.moni.payment.application.service.CommandService.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.lock.RedisSubscriptionLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribePaymentUseCase {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final SubscriptionQueryService subscriptionQueryService;
    private final PaymentCommandService paymentCommandService;
    private final TossPaymentsAdapter tossPaymentsAdapter;
    private final RedisSubscriptionLock redisSubscriptionLock;

    public SubscribeResult execute(SubscribeCommand command) {
        UUID userId = command.userId();

        if (!redisSubscriptionLock.tryLock(userId, LOCK_TTL)) {
            throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT);
        }

        try {
            subscriptionQueryService.checkNoActiveSubscription(userId);

            MerchantId merchantId = MerchantId.generate();
            Money amount = Money.of(command.amount());
            UUID paymentId = paymentCommandService.recordPendingPayment(
                    new RecordPendingPaymentCommand(
                            userId, merchantId, amount,
                            PaymentType.SUBSCRIPTION_INITIAL,
                            Instant.now().plusSeconds(600),
                            command.requestedBy()));

            TossPaymentsAdapter.PgPaymentResult pgResult;
            try {
                pgResult = tossPaymentsAdapter.requestPayment(
                        new TossPaymentsAdapter.PgPaymentRequest(
                                command.authKey(), command.customerKey(),
                                merchantId, userId, amount,
                                PaymentType.SUBSCRIPTION_INITIAL));
            } catch (Exception e) {
                paymentCommandService.failPayment(
                        new FailPaymentCommand(paymentId, "PG_ERROR", command.requestedBy()));
                throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
            }

            if (!pgResult.success()) {
                paymentCommandService.failPayment(
                        new FailPaymentCommand(paymentId, pgResult.rawResponse(), command.requestedBy()));
                throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
            }

            paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(
                            paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                            pgResult.rawResponse(), command.requestedBy()));

            log.info("구독 결제 완료: paymentId={}, userId={}", paymentId, userId);

            return new SubscribeResult(paymentId, "COMPLETED", amount.getValue().longValue(),
                    LocalDate.now().plusMonths(1));

        } finally {
            redisSubscriptionLock.unlock(userId);
        }
    }
}
