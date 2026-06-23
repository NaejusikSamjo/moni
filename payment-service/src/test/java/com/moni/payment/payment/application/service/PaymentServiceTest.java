package com.moni.payment.payment.application.service;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.payment.application.command.SubscribeCommand;
import com.moni.payment.payment.application.command.SubscribeResult;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import com.moni.payment.domain.port.PgGatewayPort;
import com.moni.payment.domain.port.SavePaymentHistoryPort;
import com.moni.payment.domain.port.SavePaymentPort;
import com.moni.payment.subscription.domain.port.in.ActivateSubscriptionUseCase;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.ArrayList;

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
    private LoadSubscriptionPort loadSubscriptionPort;
    @Mock
    private SavePaymentPort savePaymentPort;
    @Mock
    private SavePaymentHistoryPort savePaymentHistoryPort;
    @Mock
    private PgGatewayPort pgGatewayPort;
    @Mock
    private ActivateSubscriptionUseCase activateSubscriptionUseCase;

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
                loadSubscriptionPort,
                savePaymentPort,
                savePaymentHistoryPort,
                pgGatewayPort,
                activateSubscriptionUseCase);
    }

    private SubscribeCommand command() {
        return new SubscribeCommand(USER_ID, AUTH_KEY, CUSTOMER_KEY, AMOUNT, USER_ID.toString());
    }

    private PgGatewayPort.PgPaymentResult successResult() {
        return new PgGatewayPort.PgPaymentResult(PG_PAYMENT_KEY, BILLING_KEY, "{}", true);
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
            given(loadSubscriptionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(pgGatewayPort.requestPayment(any())).willReturn(successResult());
            // doAnswer로 저장 시점 상태를 기록하면서 동시에 argument를 반환한다
            doAnswer(inv -> {
                Payment p = inv.getArgument(0);
                statusAtSaveTime.add(p.getStatus());
                return p;
            }).when(savePaymentPort).save(any());
            given(activateSubscriptionUseCase.activateSubscription(any())).willReturn(activeSubscription());
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
        @DisplayName("ActivateSubscriptionUseCase가 userId, billingKey, nextBillingDate로 호출된다")
        void activatesSubscription() {
            paymentService.initiatePayment(command());

            ArgumentCaptor<ActivateSubscriptionUseCase.ActivateSubscriptionCommand> captor =
                    ArgumentCaptor.forClass(ActivateSubscriptionUseCase.ActivateSubscriptionCommand.class);
            then(activateSubscriptionUseCase).should().activateSubscription(captor.capture());

            ActivateSubscriptionUseCase.ActivateSubscriptionCommand activateCmd = captor.getValue();
            assertThat(activateCmd.userId()).isEqualTo(USER_ID);
            assertThat(activateCmd.billingKeyValue()).isEqualTo(BILLING_KEY);
            assertThat(activateCmd.nextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("PaymentHistory가 한 번 저장된다")
        void historyIsSaved() {
            paymentService.initiatePayment(command());
            then(savePaymentHistoryPort).should(times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("이미 활성 구독이 존재하는 경우")
    class ActiveSubscriptionExists {

        @Test
        @DisplayName("ACTIVE_SUBSCRIPTION_EXISTS 예외 발생, PG 호출 없음")
        void throwsExceptionWhenActiveSubscriptionExists() {
            given(loadSubscriptionPort.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(activeSubscription()));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);

            then(pgGatewayPort).should(never()).requestPayment(any());
            then(savePaymentPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("PG 결제 실패")
    class PgFailureCase {

        @BeforeEach
        void setUp() {
            given(loadSubscriptionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(savePaymentPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("PG 예외 → Payment FAILED 저장 후 예외 re-throw")
        void pgExceptionPersistsFailedPayment() {
            given(pgGatewayPort.requestPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_COMMUNICATION_ERROR));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_COMMUNICATION_ERROR);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(savePaymentPort).should(times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("PG success=false → Payment FAILED 저장 후 PG_PAYMENT_FAILED 예외 발생")
        void pgFailureResultPersistsFailedPayment() {
            given(pgGatewayPort.requestPayment(any()))
                    .willReturn(new PgGatewayPort.PgPaymentResult(null, null, "{\"error\":\"CARD_LIMIT\"}", false));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(savePaymentPort).should(times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);

            then(activateSubscriptionUseCase).should(never()).activateSubscription(any());
        }

        @Test
        @DisplayName("PG 실패 시 구독 활성화는 호출되지 않는다")
        void subscriptionNotActivatedOnPgFailure() {
            given(pgGatewayPort.requestPayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT));

            assertThatThrownBy(() -> paymentService.initiatePayment(command()))
                    .isInstanceOf(PaymentException.class);

            then(activateSubscriptionUseCase).should(never()).activateSubscription(any());
        }
    }

    @Nested
    @DisplayName("MerchantId 생성")
    class MerchantIdGeneration {

        @Test
        @DisplayName("PG 호출 시 merchantId가 유효한 형식으로 전달된다")
        void merchantIdIsValidFormat() {
            given(loadSubscriptionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
            given(pgGatewayPort.requestPayment(any())).willReturn(successResult());
            given(savePaymentPort.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(activateSubscriptionUseCase.activateSubscription(any())).willReturn(activeSubscription());

            paymentService.initiatePayment(command());

            ArgumentCaptor<PgGatewayPort.PgPaymentRequest> captor =
                    ArgumentCaptor.forClass(PgGatewayPort.PgPaymentRequest.class);
            then(pgGatewayPort).should().requestPayment(captor.capture());

            MerchantId merchantId = captor.getValue().merchantId();
            assertThat(merchantId.getValue()).startsWith("MONI");
            assertThat(merchantId.getValue()).hasSizeBetween(6, 64);
        }
    }
}
