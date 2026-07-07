package com.moni.payment.presentation.controller;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.application.dto.command.CancelSubscriptionResult;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.ReactivateSubscriptionUseCase;
import com.moni.payment.application.service.SubscribeCancelUseCase;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionQueryService subscriptionQueryService;

    @MockitoBean
    private SubscribeCancelUseCase subscribeCancelUseCase;

    @MockitoBean
    private ReactivateSubscriptionUseCase reactivateSubscriptionUseCase;

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void ACTIVE_구독_사용자_조회_시_subscribed_true를_반환한다() throws Exception {
        // given
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionStatusResult result = new SubscriptionStatusResult(
                true, subscriptionId, SubscriptionStatus.ACTIVE, LocalDate.now().plusMonths(1), 9900L);
        given(subscriptionQueryService.execute(any())).willReturn(result);

        // when / then
        mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.subscribed").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void 구독이_없는_사용자_조회_시_subscribed_false를_반환한다() throws Exception {
        // given
        given(subscriptionQueryService.execute(any())).willReturn(SubscriptionStatusResult.inactive());

        // when / then
        mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscribed").value(false));
    }

    @Test
    void 구독_취소_성공_시_200을_반환한다() throws Exception {
        // given
        UUID subscriptionId = UUID.randomUUID();
        CancelSubscriptionResult result = new CancelSubscriptionResult(
                subscriptionId, SubscriptionStatus.CANCELLING, Instant.now());
        given(subscribeCancelUseCase.execute(any())).willReturn(result);

        // when / then
        mockMvc.perform(delete("/api/v1/payments/subscriptions")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.data.status").value("CANCELLING"));
    }

    @Test
    void 구독이_없는_사용자가_취소_요청_시_404를_반환한다() throws Exception {
        // given
        given(subscribeCancelUseCase.execute(any()))
                .willThrow(new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        // when / then
        mockMvc.perform(delete("/api/v1/payments/subscriptions")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.errorClassName").value("SUB_002"));
    }

    @Test
    void SUSPENDED_구독_재활성화_성공_시_200을_반환한다() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();
        SubscribeResult result = new SubscribeResult(
                paymentId, "COMPLETED", 9900L, LocalDate.now().plusMonths(1));
        given(reactivateSubscriptionUseCase.execute(any())).willReturn(result);

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscriptions/reactivate")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void SUSPENDED_구독이_없는_사용자가_재활성화_요청_시_404를_반환한다() throws Exception {
        // given
        given(reactivateSubscriptionUseCase.execute(any()))
                .willThrow(new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscriptions/reactivate")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.errorClassName").value("SUB_002"));
    }
}
