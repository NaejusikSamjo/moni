package com.moni.trade.market.domain.repository;

import com.moni.trade.market.domain.entity.MarketHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, UUID> {

    boolean existsByDate(LocalDate date);
}
