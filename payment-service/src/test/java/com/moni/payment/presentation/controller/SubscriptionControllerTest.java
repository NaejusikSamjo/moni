package com.moni.payment.presentation.controller;

import com.moni.payment.application.dto.SubscriptionStatusResult;
import com.moni.payment.application.service.SubscriptionQueryService;
import com.moni.payment.common.config.SecurityConfig;
import com.moni.payment.common.exception.GlobalExceptionHandler;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
    private SubscriptionQueryService subscriptionQueryService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final LocalDate NEXT_BILLING = LocalDate.of(2026, 7, 19);

    private static final String GATEWAY_SECRET = System.getenv().getOrDefault("GATEWAY_SECRET", "local-secret");

    @Nested
    @DisplayName("GET /api/v1/payments/subscriptions/status")
    class GetStatus {

        @Test
        @DisplayName("구독 중이면 200과 subscribed=true 응답을 반환한다")
        void returnsOkWhenSubscribed() throws Exception {
            SubscriptionStatusResult result = new SubscriptionStatusResult(
                    true, SUBSCRIPTION_ID, SubscriptionStatus.ACTIVE, NEXT_BILLING, 9900L);
            given(subscriptionQueryService.execute(any())).willReturn(result);

            mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.subscribed").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.nextBillingDate").value("2026-07-19"))
                    .andExpect(jsonPath("$.data.amount").value(9900));
        }

        @Test
        @DisplayName("구독 중이 아니면 200과 subscribed=false 응답을 반환한다")
        void returnsOkWithInactiveWhenNotSubscribed() throws Exception {
            given(subscriptionQueryService.execute(any())).willReturn(SubscriptionStatusResult.inactive());

            mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.subscribed").value(false))
                    .andExpect(jsonPath("$.data.subscriptionId").doesNotExist())
                    .andExpect(jsonPath("$.data.status").doesNotExist());
        }

        @Test
        @DisplayName("CANCELLING 상태도 200으로 조회된다")
        void returnsOkWhenCancelling() throws Exception {
            SubscriptionStatusResult result = new SubscriptionStatusResult(
                    true, SUBSCRIPTION_ID, SubscriptionStatus.CANCELLING, NEXT_BILLING, 9900L);
            given(subscriptionQueryService.execute(any())).willReturn(result);

            mockMvc.perform(get("/api/v1/payments/subscriptions/status")
                            .header("X-Gateway-Secret", GATEWAY_SECRET)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subscribed").value(true))
                    .andExpect(jsonPath("$.data.status").value("CANCELLING"));
        }
    }
}
