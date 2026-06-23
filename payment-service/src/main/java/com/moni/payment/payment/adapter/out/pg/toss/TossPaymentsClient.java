package com.moni.payment.payment.adapter.out.pg.toss;

import com.moni.payment.payment.adapter.out.pg.toss.dto.TossBillingAuthRequest;
import com.moni.payment.payment.adapter.out.pg.toss.dto.TossBillingChargeRequest;
import com.moni.payment.payment.adapter.out.pg.toss.dto.TossPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "toss-payments",
        url = "${toss.payments.base-url:https://api.tosspayments.com}",
        configuration = TossPaymentsClientConfig.class)
public interface TossPaymentsClient {

    @PostMapping("/v1/billing/authorizations/issue")
    TossPaymentResponse issueBillingKey(@RequestBody TossBillingAuthRequest request);

    @PostMapping("/v1/billing/{billingKey}")
    TossPaymentResponse chargeBillingKey(
            @PathVariable("billingKey") String billingKey,
            @RequestBody TossBillingChargeRequest request);

    @GetMapping("/v1/payments/{paymentKey}")
    TossPaymentResponse getPayment(@PathVariable("paymentKey") String paymentKey);
}
