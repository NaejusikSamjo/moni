package com.moni.payment.application.service;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.application.command.SubscribeCommand;
import com.moni.payment.application.command.SubscribeResult;
import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.repository.PgGateway;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.ConfirmPaymentUseCase;
import com.moni.payment.application.usecase.GetPaymentHistoryUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — initiatePayment()")
class PaymentServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PgGateway pgGateway;
    @Mock
    private SubscriptionService subscriptionService;

    private PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String AUTH_KEY = "toss-auth-key-001";
    private static final String CUSTOMER_KEY = "customer-uuid-001";
    private static final long AMOUNT = 9900L;
    private static final String PG_PAYMENT_KEY = "toss_pg_key_abc";
    private static final String BILLING_KEY = "billing-key-001";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                subscriptionRepository,
                paymentRepository,
                pgGateway,
                subscriptionService);
    }

    private SubscribeCommand command() {
        return new SubscribeCommand(USER_ID, AUTH_KEY, CUSTOMER_KEY, AMOUNT, USER_ID.toString());
    }

    private PgGateway.PgPaymentResult successResult() {
        return new PgGateway.PgPaymentResult(PG_PAYMENT_KEY, BILLING_KEY, "{}", true);
    }

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                UUID.randomUUID(), USER_ID,
                BillingKey.of(BILLING_KEY),
                SubscriptionStatus.ACTIVE,
                LocalDate.now().plusMonths(1),
                now, now, 1L, List.of());
    }

    @Nested
    @DisplayName("정상 구독 결제")
    class SuccessCase {

        final List<PaymentStatus> statusAtSaveTime = new ArrayList<>();

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(pgGateway.requestPayment(any())).willReturn(successResult());
            doAnswer(inv -> {
                Payment p = inv.getArgument(0);
                statusAtSaveTime.add(p.getStatus());
                return p;
            }).when(paymentRepository).save(any());
            given(subscriptionService.activateSubscription(any())).willReturn(activeSubscription());
        }

        @Test
        @DisplayName("SubscribeResult가 paymentId, SUCCESS, amount, nextBillingDate를 반환한다")
        void returnsSubscribeResult() {
            SubscribeResult result = paymentService.initiatePayment(command());

            assertThat(result.paymentId()).isNotNull();
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.amount()).isEqualTo(AMOUNT);
            assertThat(result.nextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("Payment가 PENDING → COMPLETED 순서로 두 번 저장된다")
        void paymentSavedTwice() {
            paymentService.initiatePayment(command());
            assertThat(statusAtSaveTime).containsExactly(PaymentStatus.PENDING, PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("SubscriptionService가 userId, billingKey로 호출된다")
        void activatesSubscription() {
            paymentService.initiatePayment(command());

            ArgumentCaptor<ActivateSubscriptionCommand> captor =
                    ArgumentCaptor.forClass(ActivateSubscriptionCommand.class);
            then(subscriptionService).should().activateSubscription(captor.capture());

            ActivateSubscriptionCommand activateCmd = captor.getValue();
            assertThat(activateCmd.userId()).isEqualTo(USER_ID);
            assertThat(activateCmd.billingKeyValue()).isEqualTo(BILLING_KEY);
        }

        @Test
        @DisplayName("PaymentHistory가 한 번 저장된다")
        void historyIsSaved() {
            paymentService.initiatePayment(command());
            then(paymentRepository).should(times(1)).saveHistory(any());
        }
    }

    @Nested
    @DisplayName("이미 활성 구독이 존재하는 경우")
    class ActiveSubscriptionExists {

        @Test
        @DisplayName("ACTIVE_SUBSCRIPTION_EXISTS 예외 발생, PG 호출 없음")
        void throwsExceptionWhenActiveSubscriptionExists() {
            given(subscriptionRepository.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(activeSubscription()));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);

            then(pgGateway).should(never()).requestPayment(any());
            then(paymentRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("PG 결제 실패")
    class PgFailureCase {

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("PG 예외 → Payment FAILED 저장 후 예외 re-throw")
        void pgExceptionPersistsFailedPayment() {
            given(pgGateway.requestPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_COMMUNICATION_ERROR));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_COMMUNICATION_ERROR);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentRepository).should(times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("PG success=false → Payment FAILED 저장 후 PG_PAYMENT_FAILED 예외 발생")
        void pgFailureResultPersistsFailedPayment() {
            given(pgGateway.requestPayment(any()))
                    .willReturn(new PgGateway.PgPaymentResult(null, null, "{\"error\":\"CARD_LIMIT\"}", false));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentRepository).should(times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);

            then(subscriptionService).should(never()).activateSubscription(any());
        }

        @Test
        @DisplayName("PG 실패 시 구독 활성화는 호출되지 않는다")
        void subscriptionNotActivatedOnPgFailure() {
            given(pgGateway.requestPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class);

            then(subscriptionService).should(never()).activateSubscription(any());
        }
    }

    @Nested
    @DisplayName("MerchantId 생성")
    class MerchantIdGeneration {

        @Test
        @DisplayName("PG 호출 시 merchantId가 유효한 형식으로 전달된다")
        void merchantIdIsValidFormat() {
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(pgGateway.requestPayment(any())).willReturn(successResult());
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(subscriptionService.activateSubscription(any())).willReturn(activeSubscription());

            paymentService.initiatePayment(command());

            ArgumentCaptor<PgGateway.PgPaymentRequest> captor =
                    ArgumentCaptor.forClass(PgGateway.PgPaymentRequest.class);
            then(pgGateway).should().requestPayment(captor.capture());

            MerchantId merchantId = captor.getValue().merchantId();
            assertThat(merchantId.getValue()).startsWith("MONI");
            assertThat(merchantId.getValue()).hasSizeBetween(6, 64);
        }
    }

    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPayment {

        private static final String MERCHANT_ID_VALUE = "MONI" + "a".repeat(32);
        private static final String PG_PAYMENT_KEY = "toss_pg_confirm_001";
        private static final String PG_RESPONSE = "{\"status\":\"DONE\"}";
        private static final Instant RESPONDED_AT = Instant.now();

        private Payment pendingPayment() {
            return Payment.create(
                    USER_ID,
                    MerchantId.of(MERCHANT_ID_VALUE),
                    Money.of(9900L),
                    PaymentType.SUBSCRIPTION_INITIAL,
                    Instant.now().plusSeconds(600),
                    USER_ID.toString());
        }

        private ConfirmPaymentUseCase.ConfirmPaymentCommand confirmCommand() {
            return new ConfirmPaymentUseCase.ConfirmPaymentCommand(
                    MERCHANT_ID_VALUE, PG_PAYMENT_KEY, PG_RESPONSE, RESPONDED_AT, USER_ID.toString());
        }

        @Test
        @DisplayName("PENDING 결제 확인 성공 → COMPLETED 저장 후 히스토리 저장")
        void successCompletesPayment() {
            Payment payment = pendingPayment();
            given(paymentRepository.findByMerchantId(MerchantId.of(MERCHANT_ID_VALUE)))
                    .willReturn(Optional.of(payment));
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentService.confirmPayment(confirmCommand());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentRepository).should(times(1)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            then(paymentRepository).should(times(1)).saveHistory(any());
        }

        @Test
        @DisplayName("merchantId에 해당하는 결제가 없으면 PAYMENT_NOT_FOUND 예외 발생")
        void throwsWhenPaymentNotFound() {
            given(paymentRepository.findByMerchantId(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment(confirmCommand()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);

            then(paymentRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("멱등성: 이미 COMPLETED 상태인 결제 재확인 시 INVALID_PAYMENT_STATUS_TRANSITION 예외 발생")
        void idempotencyRejectsAlreadyCompleted() {
            Payment completed = Payment.reconstitute(
                    UUID.randomUUID(), MerchantId.of(MERCHANT_ID_VALUE), USER_ID,
                    PaymentType.SUBSCRIPTION_INITIAL, Money.of(9900L), PG_PAYMENT_KEY,
                    PaymentStatus.COMPLETED,
                    Instant.now().plusSeconds(600),
                    Instant.now(), USER_ID.toString(),
                    Instant.now(), USER_ID.toString(),
                    Collections.emptyList());
            given(paymentRepository.findByMerchantId(any())).willReturn(Optional.of(completed));

            assertThatThrownBy(() -> paymentService.confirmPayment(confirmCommand()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("userId에 해당하는 결제 목록을 반환한다")
        void returnsPaymentList() {
            Payment p1 = Payment.create(
                    USER_ID, MerchantId.of("MONI" + "b".repeat(32)),
                    Money.of(9900L), PaymentType.SUBSCRIPTION_INITIAL,
                    Instant.now().plusSeconds(600), USER_ID.toString());
            given(paymentRepository.findByUserId(USER_ID, 0, 10)).willReturn(List.of(p1));

            List<Payment> result = paymentService.getHistory(
                    new GetPaymentHistoryUseCase.GetPaymentHistoryQuery(USER_ID, 0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("결제 내역이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoHistory() {
            given(paymentRepository.findByUserId(USER_ID, 0, 10)).willReturn(Collections.emptyList());

            List<Payment> result = paymentService.getHistory(
                    new GetPaymentHistoryUseCase.GetPaymentHistoryQuery(USER_ID, 0, 10));

            assertThat(result).isEmpty();
        }
    }
}
