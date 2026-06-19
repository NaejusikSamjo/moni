package com.moni.trade.account.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.account.presentation.dto.request.AccountCreateRequestDto;
import com.moni.trade.account.presentation.dto.response.AccountResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10000000");

    private final AccountRepository accountRepository;

    public AccountResponseDto findAccountByUserId(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponseDto.from(account);
    }

    @Transactional
    public AccountResponseDto createAccount(AccountCreateRequestDto request) {
        if (accountRepository.existsByUserId(request.userId())) {
            throw new CustomException(AccountErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        Account account = Account.create(request.userId(), INITIAL_BALANCE);
        return AccountResponseDto.from(accountRepository.save(account));
    }
}
