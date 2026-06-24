package com.moni.payment.infrastructure.client.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.application.repository.PgPaymentClient;
import com.moni.payment.application.repository.PgPaymentClient.PgPaymentRequest;
import com.moni.payment.application.repository.PgPaymentClient.PgPaymentResult;
import com.moni.payment.application.repository.PgPaymentClient.PgPaymentStatus;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.client.toss.TossPaymentsClient;
import com.moni.payment.infrastructure.client.toss.dto.TossBillingAuthRequest;
import com.moni.payment.infrastructure.client.toss.dto.TossBillingChargeRequest;
import com.moni.payment.infrastructure.client.toss.dto.TossPaymentResponse;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TossPaymentsAdapter")
class TossPaymentsAdapterTest {

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TossPaymentsAdapter adapter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String AUTH_KEY = "test-auth-key";
    private static final String BILLING_KEY = "billing-key-001";
    private static final String PAYMENT_KEY = "payment-key-001";

    @BeforeEach
    void setUp() throws Exception {
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
    }

    @Nested
    @DisplayName("requestPayment()")
    class RequestPayment {

        @Test
        @DisplayName("BillingKey 발급 후 결제 성공 시 PgPaymentResult를 반환한다")
        void success() {
            TossPaymentResponse billingKeyResponse = new TossPaymentResponse(
                    null, BILLING_KEY, "ISSUED", null, null, null, null, null, null);
            TossPaymentResponse chargeResponse = new TossPaymentResponse(
                    PAYMENT_KEY, null, "DONE", 9900L, "카드", "order-001", "모니 AI 구독", null, null);

            given(tossPaymentsClient.issueBillingKey(any(TossBillingAuthRequest.class)))
                    .willReturn(billingKeyResponse);
            given(tossPaymentsClient.chargeBillingKey(anyString(), any(TossBillingChargeRequest.class)))
                    .willReturn(chargeResponse);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            PgPaymentResult result = adapter.requestPayment(request);

            assertThat(result.pgPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(result.billingKeyValue()).isEqualTo(BILLING_KEY);
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("BillingKey 발급 실패(4xx) 시 PG_PAYMENT_FAILED 예외 발생")
        void billingKeyIssueFails() {
            given(tossPaymentsClient.issueBillingKey(any()))
                    .willThrow(FeignException.FeignClientException.class);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            assertThatThrownBy(() -> adapter.requestPayment(request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        @Test
        @DisplayName("결제 결과가 DONE이 아닐 때 success=false")
        void chargeNotDone() {
            TossPaymentResponse billingKeyResponse = new TossPaymentResponse(
                    null, BILLING_KEY, "ISSUED", null, null, null, null, null, null);
            TossPaymentResponse chargeResponse = new TossPaymentResponse(
                    PAYMENT_KEY, null, "CANCELED", null, null, null, null, null, null);

            given(tossPaymentsClient.issueBillingKey(any())).willReturn(billingKeyResponse);
            given(tossPaymentsClient.chargeBillingKey(anyString(), any())).willReturn(chargeResponse);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            PgPaymentResult result = adapter.requestPayment(request);

            assertThat(result.success()).isFalse();
        }

        @Test
        @DisplayName("네트워크 타임아웃(BillingKey 발급) → PG_CONNECTION_TIMEOUT 예외 발생")
        void billingKeyTimeoutThrowsConnectionTimeout() {
            given(tossPaymentsClient.issueBillingKey(any()))
                    .willThrow(RetryableException.class);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            assertThatThrownBy(() -> adapter.requestPayment(request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        }

        @Test
        @DisplayName("서버 오류(5xx, BillingKey 발급) → PG_COMMUNICATION_ERROR 예외 발생")
        void billingKeyServerErrorThrowsCommunicationError() {
            given(tossPaymentsClient.issueBillingKey(any()))
                    .willThrow(FeignException.FeignServerException.class);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            assertThatThrownBy(() -> adapter.requestPayment(request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_COMMUNICATION_ERROR);
        }

        @Test
        @DisplayName("네트워크 타임아웃(BillingKey 결제) → PG_CONNECTION_TIMEOUT 예외 발생")
        void chargeTimeoutThrowsConnectionTimeout() {
            TossPaymentResponse billingKeyResponse = new TossPaymentResponse(
                    null, BILLING_KEY, "ISSUED", null, null, null, null, null, null);
            given(tossPaymentsClient.issueBillingKey(any())).willReturn(billingKeyResponse);
            given(tossPaymentsClient.chargeBillingKey(anyString(), any()))
                    .willThrow(RetryableException.class);

            PgPaymentRequest request = new PgPaymentRequest(
                    AUTH_KEY,
                    MerchantId.of("order-001"),
                    USER_ID,
                    Money.of(9900),
                    PaymentType.SUBSCRIPTION_INITIAL);

            assertThatThrownBy(() -> adapter.requestPayment(request))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        }
    }

    @Nested
    @DisplayName("requestBillingPayment()")
    class RequestBillingPayment {

        @Test
        @DisplayName("정기결제 성공 시 PgPaymentResult 반환")
        void success() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, "DONE", 9900L, "카드", "order-002", "모니 AI 구독", null, null);
            given(tossPaymentsClient.chargeBillingKey(anyString(), any()))
                    .willReturn(response);

            PgPaymentResult result = adapter.requestBillingPayment(
                    BILLING_KEY, Money.of(9900), MerchantId.of("order-002"));

            assertThat(result.pgPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(result.billingKeyValue()).isEqualTo(BILLING_KEY);
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("정기결제 실패(4xx) 시 PG_PAYMENT_FAILED 예외 발생")
        void billingChargeFails() {
            given(tossPaymentsClient.chargeBillingKey(anyString(), any()))
                    .willThrow(FeignException.FeignClientException.class);

            assertThatThrownBy(() ->
                    adapter.requestBillingPayment(BILLING_KEY, Money.of(9900), MerchantId.of("order-002")))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        @Test
        @DisplayName("정기결제 타임아웃 → PG_CONNECTION_TIMEOUT 예외 발생")
        void billingChargeTimeout() {
            given(tossPaymentsClient.chargeBillingKey(anyString(), any()))
                    .willThrow(RetryableException.class);

            assertThatThrownBy(() ->
                    adapter.requestBillingPayment(BILLING_KEY, Money.of(9900), MerchantId.of("order-002")))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        }
    }

    @Nested
    @DisplayName("inquirePayment()")
    class InquirePayment {

        @Test
        @DisplayName("DONE 상태 조회 시 APPROVED 반환")
        void doneReturnsApproved() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, "DONE", 9900L, "카드", null, null, null, null);
            given(tossPaymentsClient.getPayment(PAYMENT_KEY)).willReturn(response);

            PgPaymentStatus status = adapter.inquirePayment(PAYMENT_KEY);

            assertThat(status).isEqualTo(PgPaymentStatus.APPROVED);
        }

        @Test
        @DisplayName("CANCELED 상태 조회 시 CANCELED 반환")
        void canceledReturnsCanceled() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, "CANCELED", null, null, null, null, null, null);
            given(tossPaymentsClient.getPayment(PAYMENT_KEY)).willReturn(response);

            PgPaymentStatus status = adapter.inquirePayment(PAYMENT_KEY);

            assertThat(status).isEqualTo(PgPaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("조회 실패(4xx) 시 PG_PAYMENT_FAILED 예외 발생")
        void inquireFails() {
            given(tossPaymentsClient.getPayment(PAYMENT_KEY))
                    .willThrow(FeignException.FeignClientException.class);

            assertThatThrownBy(() -> adapter.inquirePayment(PAYMENT_KEY))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        @Test
        @DisplayName("PG사 에러코드 매핑: WAITING_FOR_DEPOSIT → WAITING_FOR_DEPOSIT")
        void waitingForDepositMapped() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, "WAITING_FOR_DEPOSIT", null, null, null, null, null, null);
            given(tossPaymentsClient.getPayment(PAYMENT_KEY)).willReturn(response);

            PgPaymentStatus status = adapter.inquirePayment(PAYMENT_KEY);

            assertThat(status).isEqualTo(PgPaymentStatus.WAITING_FOR_DEPOSIT);
        }

        @Test
        @DisplayName("PG사 에러코드 매핑: 미지원 상태 → FAILED")
        void unknownStatusMappedToFailed() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, "PARTIAL_CANCELED", null, null, null, null, null, null);
            given(tossPaymentsClient.getPayment(PAYMENT_KEY)).willReturn(response);

            PgPaymentStatus status = adapter.inquirePayment(PAYMENT_KEY);

            assertThat(status).isEqualTo(PgPaymentStatus.FAILED);
        }

        @Test
        @DisplayName("PG사 에러코드 매핑: null 상태 → FAILED")
        void nullStatusMappedToFailed() {
            TossPaymentResponse response = new TossPaymentResponse(
                    PAYMENT_KEY, null, null, null, null, null, null, null, null);
            given(tossPaymentsClient.getPayment(PAYMENT_KEY)).willReturn(response);

            PgPaymentStatus status = adapter.inquirePayment(PAYMENT_KEY);

            assertThat(status).isEqualTo(PgPaymentStatus.FAILED);
        }

        @Test
        @DisplayName("서버 오류(5xx) 시 PG_COMMUNICATION_ERROR 예외 발생")
        void inquireServerError() {
            given(tossPaymentsClient.getPayment(PAYMENT_KEY))
                    .willThrow(FeignException.FeignServerException.class);

            assertThatThrownBy(() -> adapter.inquirePayment(PAYMENT_KEY))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_COMMUNICATION_ERROR);
        }

        @Test
        @DisplayName("조회 타임아웃 → PG_CONNECTION_TIMEOUT 예외 발생")
        void inquireTimeout() {
            given(tossPaymentsClient.getPayment(PAYMENT_KEY))
                    .willThrow(RetryableException.class);

            assertThatThrownBy(() -> adapter.inquirePayment(PAYMENT_KEY))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        }
    }
}
