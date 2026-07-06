package com.moni.payment.application.scheduler;

import com.moni.payment.application.service.SubscribeScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final SubscribeScheduleUseCase subscribeScheduleUseCase;

    @Scheduled(cron = "0 0 9 * * *")
    public void chargeActiveSubscriptions() {
        log.info("정기결제 스케줄러 시작");
        subscribeScheduleUseCase.execute();
        log.info("정기결제 스케줄러 완료");
    }
}
