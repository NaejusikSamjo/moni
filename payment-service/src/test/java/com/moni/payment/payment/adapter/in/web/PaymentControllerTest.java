package com.moni.payment.payment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.payment.common.config.SecurityConfig;
import com.moni.payment.common.exception.GlobalExceptionHandler;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.payment.adapter.in.web.dto.SubscribeRequest;
import com.moni.payment.application.command.SubscribeResult;
import com.moni.payment.application.usecase.InitiatePaymentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InitiatePaymentUseCase initiatePaymentUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final LocalDate NEXT_BILLING = LocalDate.of(2026, 7, 19);
    private static final String GATEWAY_SECRET = System.getenv().getOrDefault("GATEWAY_SECRET", "local-secret");

    private SubscribeRequest validRequest() {
        return new SubscribeRequest("toss-auth-key-001", "customer-uuid-001", 9900L, "모니 AI 구독");
    }

    private SubscribeResult successResult() {
        return new SubscribeResult(PAYMENT_ID, "COMPLETED", 9900L, NEXT_BILLING);
    }

    @Nested
    @DisplayName("POST /api/v1/payments/subscription")
    class Subscribe {

        @Test
        @DisplayName("정상 결제 시 201과 응답 DTO를 반환한다")
        void returnsCreated() throws Exception {
            given(initiatePaymentUseCase.initiatePayment(any())).willReturn(successResult());

            mockMvc.perform(post("/api/v1/payments/subscription")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID.toString()))
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.amount").value(9900))
                    .andExpect(jsonPath("$.data.nextBillingDate").value("2026-07-19"));
        }

        @Test
        @DisplayName("이미 활성 구독이 있으면 409를 반환한다")
        void returnsConflictWhenActiveSubscriptionExists() throws Exception {
            given(initiatePaymentUseCase.initiatePayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS));

            mockMvc.perform(post("/api/v1/payments/subscription")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors.errorClassName").value("SUB_003"));
        }

        @Test
        @DisplayName("PG 결제 실패 시 422를 반환한다")
        void returnsUnprocessableWhenPgFails() throws Exception {
            given(initiatePaymentUseCase.initiatePayment(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED));

            mockMvc.perform(post("/api/v1/payments/subscription")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.errorClassName").value("PG_002"));
        }

        @Test
        @DisplayName("X-User-Id 헤더 없으면 인증 실패로 403을 반환한다")
        void returnsForbiddenWhenUserIdMissing() throws Exception {
            mockMvc.perform(post("/api/v1/payments/subscription")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("authKey가 비어있으면 400을 반환한다")
        void returnsBadRequestWhenAuthKeyBlank() throws Exception {
            SubscribeRequest invalidRequest = new SubscribeRequest("", "customer-uuid-001", 9900L, "모니 AI 구독");

            mockMvc.perform(post("/api/v1/payments/subscription")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
