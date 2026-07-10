package com.moni.trade.holding.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.exception.HoldingErrorCode;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.holding.presentation.dto.response.HoldingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;

    public PageRes<HoldingResponseDto> findHoldings(UUID userId, Pageable pageable) {
        Account account = getAccountByUserId(userId);
        return new PageRes<>(
                holdingRepository.findByAccountId(account.getId(), pageable)
                        .map(HoldingResponseDto::from)
        );
    }

    public HoldingResponseDto findHolding(UUID userId, String ticker) {
        Account account = getAccountByUserId(userId);
        Holding holding = holdingRepository.findByAccountIdAndTicker(account.getId(), ticker)
                .orElseThrow(() -> new CustomException(HoldingErrorCode.HOLDING_NOT_FOUND));
        return HoldingResponseDto.from(holding);
    }

    private Account getAccountByUserId(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }
}
