package com.moni.trade.trade.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.market.application.service.MarketService;
import com.moni.trade.trade.domain.exception.TradeErrorCode;
import com.moni.trade.trade.domain.repository.TradeRepository;
import com.moni.trade.trade.presentation.dto.request.TradeBuyRequestDto;
import com.moni.trade.trade.presentation.dto.request.TradeSellRequestDto;
import com.moni.trade.trade.presentation.dto.response.TradeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {

    private static final int MAX_RETRY = 3;

    private final TradeExecutor tradeExecutor;
    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final MarketService marketService;

    public TradeResponseDto buyStock(UUID userId, TradeBuyRequestDto request) {
        marketService.validateMarketOpen();
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return tradeExecutor.executeBuy(userId, request);
            } catch (ObjectOptimisticLockingFailureException ignored) {
                // 충돌 시 재시도
            }
        }
        throw new CustomException(TradeErrorCode.TRADE_CONCURRENT_CONFLICT);
    }

    public TradeResponseDto sellStock(UUID userId, TradeSellRequestDto request) {
        marketService.validateMarketOpen();
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return tradeExecutor.executeSell(userId, request);
            } catch (ObjectOptimisticLockingFailureException ignored) {
                // 충돌 시 재시도
            }
        }
        throw new CustomException(TradeErrorCode.TRADE_CONCURRENT_CONFLICT);
    }

    @Transactional(readOnly = true)
    public PageRes<TradeResponseDto> findTrades(UUID userId, Pageable pageable) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        return new PageRes<>(
                tradeRepository.findByAccountId(account.getId(), pageable)
                        .map(TradeResponseDto::from)
        );
    }
}
