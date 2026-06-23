package com.moni.payment.presentation.controller;

import com.moni.payment.common.config.SecurityConfig;
import com.moni.payment.common.exception.GlobalExceptionHandler;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.application.usecase.GetSubscriptionStatusUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("SubscriptionController")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSubscriptionStatusUseCase getSubscriptionStatusUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate NEXT_BILLING = LocalDate.of(2026, 7, 19);

    private static final String GATEWAY_SECRET = System.getenv().getOrDefault("GATEWAY_SECRET", "local-secret");

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                UUID.randomUUID(),
                USER_ID,
                BillingKey.of("bk-test-001"),
                SubscriptionStatus.ACTIVE,
                NEXT_BILLING,
                now, now, 1L, List.of());
    }

    @Nested
    @DisplayName("GET /api/v1/payments/subscriptions/status")
    class GetStatus {

        @Test
        @DisplayName("구독이 존재하면 200과 응답 DTO를 반환한다")
        void returnsOk() throws Exception {
            given(getSubscriptionStatusUseCase.getSubscriptionStatus(any()))
                    .willReturn(activeSubscription());

            mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.nextBillingDate").value("2026-07-19"))
                    .andExpect(jsonPath("$.data.amount").value(9900));
        }

        @Test
        @DisplayName("구독이 없으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            given(getSubscriptionStatusUseCase.getSubscriptionStatus(any()))
                    .willThrow(new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

            mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors.errorClassName").value("SUB_002"));
        }
    }
}
