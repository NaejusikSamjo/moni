package com.moni.payment.application.service;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.application.command.SubscribeCommand;
import com.moni.payment.application.command.SubscribeResult;
import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.repository.PgGateway;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.ConfirmPaymentUseCase;
import com.moni.payment.application.usecase.GetPaymentHistoryUseCase;
import com.moni.payment.application.usecase.InitiatePaymentUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.model.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements InitiatePaymentUseCase, ConfirmPaymentUseCase, GetPaymentHistoryUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PgGateway pgGateway;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public SubscribeResult initiatePayment(SubscribeCommand command) {
        // 1. 이미 활성 구독 여부 확인
        subscriptionRepository.findActiveByUserId(command.userId())
                .ifPresent(existing -> {
                    throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
                });

        // 2. MerchantId 생성 (UUID 기반)
        MerchantId merchantId = MerchantId.of(
                "MONI" + UUID.randomUUID().toString().replace("-", ""));

        // 3. Payment(PENDING) 생성 후 저장
        Money amount = Money.of(command.amount());
        Payment payment = Payment.initiate(
                command.userId(), merchantId, amount,
                PaymentType.SUBSCRIPTION_INITIAL,
                Instant.now().plusSeconds(600),
                command.requestedBy());
        paymentRepository.save(payment);
        log.info("결제 PENDING 저장: paymentId={}, userId={}", payment.getId(), command.userId());

        // 4+5. PG API 호출 (트랜잭션 외부에서 실행이 이상적이나, 현재는 단일 트랜잭션 범위)
        PgGateway.PgPaymentRequest pgRequest = new PgGateway.PgPaymentRequest(
                command.authKey(), merchantId, command.userId(), amount, PaymentType.SUBSCRIPTION_INITIAL);

        PgGateway.PgPaymentResult pgResult;
        try {
            pgResult = pgGateway.requestPayment(pgRequest);
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

        // 6-A: 성공 → COMPLETED 저장
        payment.complete(pgResult.pgPaymentKey(), pgResult.rawResponse(), Instant.now(), command.requestedBy());
        paymentRepository.save(payment);
        List<PaymentHistory> histories = payment.getHistories();
        paymentRepository.saveHistory(histories.get(histories.size() - 1));
        log.info("결제 COMPLETED 저장: paymentId={}", payment.getId());

        // 7+8. 구독 활성화 및 이벤트 발행
        LocalDate nextBillingDate = LocalDate.now().plusMonths(1);
        subscriptionService.activateSubscription(
                new ActivateSubscriptionCommand(
                        command.userId(), pgResult.billingKeyValue(), nextBillingDate));

        return new SubscribeResult(
                payment.getId(),
                payment.getStatus().name(),
                payment.getAmount().getValue().longValue(),
                nextBillingDate);
    }

    @Override
    @Transactional
    public void confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByMerchantId(MerchantId.of(command.merchantId()))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        payment.complete(command.pgPaymentKey(), command.pgResponse(), command.respondedAt(), command.confirmedBy());
        paymentRepository.save(payment);
        List<PaymentHistory> histories = payment.getHistories();
        paymentRepository.saveHistory(histories.get(histories.size() - 1));
        log.info("결제 CONFIRM 저장: paymentId={}", payment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getHistory(GetPaymentHistoryQuery query) {
        return paymentRepository.findByUserId(query.userId(), query.page(), query.size());
    }

    private void persistPaymentFailure(Payment payment, String pgResponse, String actor) {
        payment.fail(pgResponse, Instant.now(), actor);
        paymentRepository.save(payment);
        List<PaymentHistory> histories = payment.getHistories();
        paymentRepository.saveHistory(histories.get(histories.size() - 1));
        log.warn("결제 FAILED 저장: paymentId={}", payment.getId());
    }
}
