package com.moni.payment.payment.application.service;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.payment.application.command.SubscribeCommand;
import com.moni.payment.payment.application.command.SubscribeResult;
import com.moni.payment.payment.domain.model.MerchantId;
import com.moni.payment.payment.domain.model.Money;
import com.moni.payment.payment.domain.model.Payment;
import com.moni.payment.payment.domain.model.PaymentHistory;
import com.moni.payment.payment.domain.model.PaymentType;
import com.moni.payment.payment.domain.port.in.InitiatePaymentUseCase;
import com.moni.payment.payment.domain.port.out.PgGatewayPort;
import com.moni.payment.payment.domain.port.out.SavePaymentHistoryPort;
import com.moni.payment.payment.domain.port.out.SavePaymentPort;
import com.moni.payment.subscription.domain.port.in.ActivateSubscriptionUseCase;
import com.moni.payment.subscription.domain.port.out.LoadSubscriptionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements InitiatePaymentUseCase {

    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SavePaymentPort savePaymentPort;
    private final SavePaymentHistoryPort savePaymentHistoryPort;
    private final PgGatewayPort pgGatewayPort;
    private final ActivateSubscriptionUseCase activateSubscriptionUseCase;

    @Override
    public SubscribeResult initiatePayment(SubscribeCommand command) {
        // 1. 이미 활성 구독 여부 확인
        loadSubscriptionPort.findActiveByUserId(command.userId())
                .ifPresent(existing -> {
                    throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
                });

        // 2. MerchantId 생성 (UUID 기반)
        MerchantId merchantId = MerchantId.of(
                "MONI" + UUID.randomUUID().toString().replace("-", ""));

        // 3. Payment(PENDING) 생성 후 저장 (Tx1 - 별도 커밋)
        Money amount = Money.of(command.amount());
        Payment payment = Payment.initiate(
                command.userId(), merchantId, amount,
                PaymentType.SUBSCRIPTION_INITIAL,
                Instant.now().plusSeconds(600),
                command.requestedBy());
        savePaymentPort.save(payment);
        log.info("결제 PENDING 저장: paymentId={}, userId={}", payment.getId(), command.userId());

        // 4+5. PG API 호출 (트랜잭션 외부)
        PgGatewayPort.PgPaymentRequest pgRequest = new PgGatewayPort.PgPaymentRequest(
                command.authKey(), merchantId, command.userId(), amount, PaymentType.SUBSCRIPTION_INITIAL);

        PgGatewayPort.PgPaymentResult pgResult;
        try {
            pgResult = pgGatewayPort.requestPayment(pgRequest);
        } catch (PaymentException e) {
            // 6-B: PG 예외 → FAILED 저장 후 re-throw
            persistPaymentFailure(payment, "PG_ERROR", command.requestedBy());
            throw e;
        }

        if (!pgResult.success()) {
            // 6-B: PG 결과 실패 → FAILED 저장
            persistPaymentFailure(payment, pgResult.rawResponse(), command.requestedBy());
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        // 6-A: 성공 → COMPLETED 저장 (Tx2 - 별도 커밋)
        payment.complete(pgResult.pgPaymentKey(), pgResult.rawResponse(), Instant.now(), command.requestedBy());
        savePaymentPort.save(payment);
        List<PaymentHistory> histories = payment.getHistories();
        savePaymentHistoryPort.save(histories.get(histories.size() - 1));
        log.info("결제 COMPLETED 저장: paymentId={}", payment.getId());

        // 7+8. 구독 활성화 및 이벤트 발행
        LocalDate nextBillingDate = LocalDate.now().plusMonths(1);
        activateSubscriptionUseCase.activateSubscription(
                new ActivateSubscriptionUseCase.ActivateSubscriptionCommand(
                        command.userId(), pgResult.billingKeyValue(), nextBillingDate));

        return new SubscribeResult(
                payment.getId(),
                payment.getStatus().name(),
                payment.getAmount().getValue().longValue(),
                nextBillingDate);
    }

    private void persistPaymentFailure(Payment payment, String pgResponse, String actor) {
        payment.fail(pgResponse, Instant.now(), actor);
        savePaymentPort.save(payment);
        List<PaymentHistory> histories = payment.getHistories();
        savePaymentHistoryPort.save(histories.get(histories.size() - 1));
        log.warn("결제 FAILED 저장: paymentId={}", payment.getId());
    }
}
