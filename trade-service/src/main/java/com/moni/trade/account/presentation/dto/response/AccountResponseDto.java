package com.moni.trade.account.presentation.dto.response;

import com.moni.trade.account.domain.entity.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponseDto(
        UUID id,
        UUID userId,
        BigDecimal balance,
        BigDecimal totalInvestment
) {
    public static AccountResponseDto from(Account account) {
        return new AccountResponseDto(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getTotalInvestment()
        );
    }
}
