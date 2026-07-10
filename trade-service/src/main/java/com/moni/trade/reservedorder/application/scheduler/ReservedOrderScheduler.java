package com.moni.trade.reservedorder.application.scheduler;

import com.moni.trade.market.application.service.MarketService;
import com.moni.trade.reservedorder.application.service.ReservedOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservedOrderScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReservedOrderService reservedOrderService;
    private final MarketService marketService;

    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Seoul")
    public void processReservationOrders() {
        LocalDate today = LocalDate.now(KST);
        if (marketService.isHoliday(today)) {
            log.info("공휴일로 예약 주문 처리 건너뜀 date={}", today);
            return;
        }
        log.info("예약 주문 처리 시작 date={}", today);
        reservedOrderService.processReservationOrders();
        log.info("예약 주문 처리 완료 date={}", today);
    }

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void checkLimitOrders() {
        if (!marketService.isMarketOpen()) {
            return;
        }
        reservedOrderService.processLimitOrders();
    }
}
