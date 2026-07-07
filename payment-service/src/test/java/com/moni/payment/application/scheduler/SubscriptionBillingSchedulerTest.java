package com.moni.payment.application.scheduler;

import com.moni.payment.application.service.SubscribeScheduleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionBillingScheduler 테스트")
class SubscriptionBillingSchedulerTest {

    @Mock private SubscribeScheduleUseCase subscribeScheduleUseCase;

    @InjectMocks
    private SubscriptionBillingScheduler subscriptionBillingScheduler;

    @Test
    @DisplayName("chargeActiveSubscriptions() 호출 시 subscribeScheduleUseCase.execute()를 위임한다")
    void 정기결제_스케줄러는_subscribeScheduleUseCase_execute를_위임한다() {
        subscriptionBillingScheduler.chargeActiveSubscriptions();

        then(subscribeScheduleUseCase).should().execute();
    }
}
