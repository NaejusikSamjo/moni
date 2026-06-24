package com.moni.trade.trade.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.exception.HoldingErrorCode;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.trade.domain.entity.Trade;
import com.moni.trade.trade.domain.repository.TradeRepository;
import com.moni.trade.trade.infrastructure.client.StockServiceClient;
import com.moni.trade.trade.infrastructure.message.TradeEventPublisher;
import com.moni.trade.trade.infrastructure.message.event.TradeCompletedEvent;
import com.moni.trade.trade.presentation.dto.request.TradeBuyRequestDto;
import com.moni.trade.trade.presentation.dto.request.TradeSellRequestDto;
import com.moni.trade.trade.presentation.dto.response.TradeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final StockServiceClient stockServiceClient;
    private final TradeEventPublisher tradeEventPublisher;

    @Transactional
    public TradeResponseDto buyStock(UUID userId, TradeBuyRequestDto request) {
        Account account = getAccountByUserId(userId);
        BigDecimal currentPrice = stockServiceClient.getStock(request.ticker()).price();
        BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(request.quantity()));

        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new CustomException(AccountErrorCode.INSUFFICIENT_BALANCE);
        }

        Trade trade = Trade.createBuy(account.getId(), request.ticker(), request.quantity(), currentPrice);
        tradeRepository.save(trade);

        account.deductBalance(totalAmount);

        holdingRepository.findByAccountIdAndTicker(account.getId(), request.ticker())
                .ifPresentOrElse(
                        holding -> holding.buy(request.quantity(), currentPrice),
                        () -> holdingRepository.save(
                                Holding.create(account.getId(), request.ticker(), request.quantity(), currentPrice)
                        )
                );

        trade.complete();

        tradeEventPublisher.publishTradeCompleted(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), null, null
        ));

        return TradeResponseDto.from(trade);
    }

    @Transactional
    public TradeResponseDto sellStock(UUID userId, TradeSellRequestDto request) {
        Account account = getAccountByUserId(userId);
        Holding holding = holdingRepository.findByAccountIdAndTicker(account.getId(), request.ticker())
                .orElseThrow(() -> new CustomException(HoldingErrorCode.HOLDING_NOT_FOUND));

        if (holding.getQuantity() < request.quantity()) {
            throw new CustomException(HoldingErrorCode.INSUFFICIENT_QUANTITY);
        }

        BigDecimal currentPrice = stockServiceClient.getStock(request.ticker()).price();
        BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal profitAmount = currentPrice.subtract(holding.getAveragePrice())
                .multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal profitRate = currentPrice.subtract(holding.getAveragePrice())
                .divide(holding.getAveragePrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        Trade trade = Trade.createSell(account.getId(), request.ticker(), request.quantity(),
                currentPrice, profitAmount, profitRate);
        tradeRepository.save(trade);

        account.addBalance(totalAmount);
        holding.sell(request.quantity());

        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        }

        trade.complete();

        tradeEventPublisher.publishTradeCompleted(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), profitAmount, profitRate
        ));

        return TradeResponseDto.from(trade);
    }

    public PageRes<TradeResponseDto> findTrades(UUID userId, Pageable pageable) {
        Account account = getAccountByUserId(userId);
        return new PageRes<>(
                tradeRepository.findByAccountId(account.getId(), pageable)
                        .map(TradeResponseDto::from)
        );
    }

    private Account getAccountByUserId(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }
}
