package com.moni.payment.payment.adapter.out.pg.toss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.payment.adapter.out.pg.toss.dto.TossBillingAuthRequest;
import com.moni.payment.payment.adapter.out.pg.toss.dto.TossBillingChargeRequest;
import com.moni.payment.payment.adapter.out.pg.toss.dto.TossPaymentResponse;
import com.moni.payment.payment.domain.model.MerchantId;
import com.moni.payment.payment.domain.model.Money;
import com.moni.payment.payment.domain.port.out.PgGatewayPort;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsAdapter implements PgGatewayPort {

    private static final String ORDER_NAME = "모니 AI 구독";
    private static final String STATUS_DONE = "DONE";

    private final TossPaymentsClient tossPaymentsClient;
    private final ObjectMapper objectMapper;

    @Override
    public PgPaymentResult requestPayment(PgPaymentRequest request) {
        TossBillingAuthRequest authRequest = new TossBillingAuthRequest(
                request.authKey(),
                request.userId().toString());

        TossPaymentResponse billingKeyResponse = callIssueBillingKey(authRequest);
        String billingKey = billingKeyResponse.billingKey();

        TossBillingChargeRequest chargeRequest = new TossBillingChargeRequest(
                request.userId().toString(),
                request.amount().getValue().longValue(),
                request.merchantId().getValue(),
                ORDER_NAME);

        TossPaymentResponse chargeResponse = callChargeBillingKey(billingKey, chargeRequest);

        return new PgPaymentResult(
                chargeResponse.paymentKey(),
                billingKey,
                toJson(chargeResponse),
                STATUS_DONE.equals(chargeResponse.status()));
    }

    @Override
    public PgPaymentResult requestBillingPayment(String billingKeyValue, Money amount, MerchantId merchantId) {
        TossBillingChargeRequest chargeRequest = new TossBillingChargeRequest(
                null,
                amount.getValue().longValue(),
                merchantId.getValue(),
                ORDER_NAME);

        TossPaymentResponse response = callChargeBillingKey(billingKeyValue, chargeRequest);

        return new PgPaymentResult(
                response.paymentKey(),
                billingKeyValue,
                toJson(response),
                STATUS_DONE.equals(response.status()));
    }

    @Override
    public PgPaymentStatus inquirePayment(String pgPaymentKey) {
        try {
            TossPaymentResponse response = tossPaymentsClient.getPayment(pgPaymentKey);
            return mapStatus(response.status());
        } catch (FeignException.FeignClientException e) {
            log.warn("TossPayments 결제 조회 실패: key={}, status={}", pgPaymentKey, e.status());
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        } catch (RetryableException e) {
            log.error("TossPayments 연결 타임아웃: key={}", pgPaymentKey, e);
            throw new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        } catch (FeignException e) {
            log.error("TossPayments 통신 오류: key={}", pgPaymentKey, e);
            throw new PaymentException(PaymentErrorCode.PG_COMMUNICATION_ERROR);
        }
    }

    private TossPaymentResponse callIssueBillingKey(TossBillingAuthRequest request) {
        try {
            return tossPaymentsClient.issueBillingKey(request);
        } catch (FeignException.FeignClientException e) {
            log.warn("BillingKey 발급 실패: authKey={}, status={}", request.authKey(), e.status());
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        } catch (RetryableException e) {
            log.error("TossPayments 연결 타임아웃 (BillingKey 발급)", e);
            throw new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        } catch (FeignException e) {
            log.error("TossPayments 통신 오류 (BillingKey 발급)", e);
            throw new PaymentException(PaymentErrorCode.PG_COMMUNICATION_ERROR);
        }
    }

    private TossPaymentResponse callChargeBillingKey(String billingKey, TossBillingChargeRequest request) {
        try {
            return tossPaymentsClient.chargeBillingKey(billingKey, request);
        } catch (FeignException.FeignClientException e) {
            log.warn("BillingKey 결제 실패: billingKey={}, status={}", billingKey, e.status());
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        } catch (RetryableException e) {
            log.error("TossPayments 연결 타임아웃 (BillingKey 결제)", e);
            throw new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT);
        } catch (FeignException e) {
            log.error("TossPayments 통신 오류 (BillingKey 결제)", e);
            throw new PaymentException(PaymentErrorCode.PG_COMMUNICATION_ERROR);
        }
    }

    private PgPaymentStatus mapStatus(String tossStatus) {
        if (tossStatus == null) {
            return PgPaymentStatus.FAILED;
        }
        return switch (tossStatus) {
            case "DONE" -> PgPaymentStatus.APPROVED;
            case "CANCELED" -> PgPaymentStatus.CANCELED;
            case "WAITING_FOR_DEPOSIT" -> PgPaymentStatus.WAITING_FOR_DEPOSIT;
            default -> PgPaymentStatus.FAILED;
        };
    }

    private String toJson(TossPaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("TossPaymentResponse JSON 직렬화 실패", e);
            return "{}";
        }
    }
}
