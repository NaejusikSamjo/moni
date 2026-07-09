package com.moni.trade.market.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "p_market_holiday")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketHoliday extends TradeBaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(length = 50)
    private String description;

    public static MarketHoliday of(LocalDate date, String description) {
        MarketHoliday holiday = new MarketHoliday();
        holiday.date = date;
        holiday.description = description;
        return holiday;
    }
}
