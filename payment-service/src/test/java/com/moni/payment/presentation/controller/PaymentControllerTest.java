package com.moni.payment.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.common.response.paging.PageRes;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.PaymentQueryService;
import com.moni.payment.application.service.SubscribePaymentUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.presentation.dto.PaymentHistoryResponse;
import com.moni.payment.presentation.dto.SubscribeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscribePaymentUseCase subscribePaymentUseCase;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void 구독_결제_성공_시_201_Created를_반환한다() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();
        SubscribeResult result = new SubscribeResult(
                paymentId, "COMPLETED", 9900L, LocalDate.now().plusMonths(1));
        given(subscribePaymentUseCase.execute(any())).willReturn(result);

        String requestBody = objectMapper.writeValueAsString(
                new SubscribeRequest("auth-key-123", "customer-key-456", 9900L, "AI 분석 구독"));

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscription")
                        .header("X-User-Id", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(9900));
    }

    @Test
    void authKey가_빈값이면_400을_반환한다() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new SubscribeRequest("", "customer-key-456", 9900L, "AI 분석 구독"));

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscription")
                        .header("X-User-Id", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.errorClassName").value("VALIDATION_ERROR"));
    }

    @Test
    void amount가_0이면_400을_반환한다() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new SubscribeRequest("auth-key-123", "customer-key-456", 0L, "AI 분석 구독"));

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscription")
                        .header("X-User-Id", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.errorClassName").value("VALIDATION_ERROR"));
    }

    @Test
    void 이미_활성_구독이_있으면_409를_반환한다() throws Exception {
        // given
        given(subscribePaymentUseCase.execute(any()))
                .willThrow(new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS));

        String requestBody = objectMapper.writeValueAsString(
                new SubscribeRequest("auth-key-123", "customer-key-456", 9900L, "AI 분석 구독"));

        // when / then
        mockMvc.perform(post("/api/v1/payments/subscription")
                        .header("X-User-Id", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.errorClassName").value("SUB_003"));
    }

    @Test
    void 결제_내역_조회_성공_시_200과_빈_목록을_반환한다() throws Exception {
        // given
        given(paymentQueryService.execute(any(), any()))
                .willReturn(new PageRes<>(new PageImpl<>(List.<PaymentHistoryResponse>of())));

        // when / then
        mockMvc.perform(get("/api/v1/payments")
                        .header("X-User-Id", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
