package com.moni.trade.market.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.market.domain.exception.MarketErrorCode;
import com.moni.trade.market.domain.repository.MarketHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    // TODO : 15:30분 장마감으로 추후 변경
    private static final LocalTime MARKET_CLOSE = LocalTime.of(23, 30);

    private final MarketHolidayRepository marketHolidayRepository;

    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        DayOfWeek day = now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        if (marketHolidayRepository.existsByDate(now.toLocalDate())) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }

    public boolean isHoliday(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return true;
        }
        return marketHolidayRepository.existsByDate(date);
    }

    public void validateMarketOpen() {
        if (!isMarketOpen()) {
            throw new CustomException(MarketErrorCode.MARKET_CLOSED);
        }
    }
}
