package com.moni.trade.trade.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.exception.HoldingErrorCode;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.trade.domain.entity.Trade;
import com.moni.trade.trade.domain.exception.TradeErrorCode;
import com.moni.trade.trade.domain.repository.TradeRepository;
import com.moni.trade.trade.infrastructure.client.StockServiceClient;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import com.moni.trade.trade.infrastructure.message.event.TradeCompletedEvent;
import com.moni.trade.trade.presentation.dto.request.TradeBuyRequestDto;
import com.moni.trade.trade.presentation.dto.request.TradeSellRequestDto;
import com.moni.trade.trade.presentation.dto.response.TradeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TradeExecutor {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final StockServiceClient stockServiceClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public TradeResponseDto executeBuy(UUID userId, TradeBuyRequestDto request) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        BigDecimal currentPrice = getCurrentPrice(request.ticker());
        BigDecimal quantity = request.amount().divide(currentPrice, 2, RoundingMode.DOWN);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(TradeErrorCode.INSUFFICIENT_AMOUNT);
        }

        BigDecimal actualCost = quantity.multiply(currentPrice).setScale(2, RoundingMode.DOWN);

        if (account.getBalance().compareTo(actualCost) < 0) {
            throw new CustomException(AccountErrorCode.INSUFFICIENT_BALANCE);
        }

        Trade trade = Trade.createBuy(account.getId(), request.ticker(), quantity, currentPrice);
        tradeRepository.save(trade);
        account.deductBalance(actualCost);

        holdingRepository.findByAccountIdAndTicker(account.getId(), request.ticker())
                .ifPresentOrElse(
                        holding -> holding.buy(quantity, currentPrice),
                        () -> holdingRepository.save(
                                Holding.create(account.getId(), request.ticker(), quantity, currentPrice)
                        )
                );

        trade.complete();
        applicationEventPublisher.publishEvent(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), null, null
        ));

        return TradeResponseDto.from(trade);
    }

    @Transactional
    public TradeResponseDto executeSell(UUID userId, TradeSellRequestDto request) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        Holding holding = holdingRepository.findByAccountIdAndTicker(account.getId(), request.ticker())
                .orElseThrow(() -> new CustomException(HoldingErrorCode.HOLDING_NOT_FOUND));

        if (holding.getQuantity().compareTo(request.quantity()) < 0) {
            throw new CustomException(HoldingErrorCode.INSUFFICIENT_QUANTITY);
        }

        BigDecimal currentPrice = getCurrentPrice(request.ticker());
        BigDecimal totalAmount = currentPrice.multiply(request.quantity()).setScale(2, RoundingMode.DOWN);
        BigDecimal profitAmount = currentPrice.subtract(holding.getAveragePrice())
                .multiply(request.quantity())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitRate = currentPrice.subtract(holding.getAveragePrice())
                .divide(holding.getAveragePrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        Trade trade = Trade.createSell(account.getId(), request.ticker(), request.quantity(),
                currentPrice, profitAmount, profitRate);
        tradeRepository.save(trade);

        account.addBalance(totalAmount);
        holding.sell(request.quantity());
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        }

        trade.complete();
        applicationEventPublisher.publishEvent(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), profitAmount, profitRate
        ));

        return TradeResponseDto.from(trade);
    }

    public BigDecimal getCurrentPrice(String ticker) {
        ExternalApiResponseDto<StockPriceResponseDto> response = stockServiceClient.getStock(ticker);
        if (response == null || response.data() == null
                || response.data().price() == null
                || response.data().price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(TradeErrorCode.STOCK_PRICE_FETCH_FAILED);
        }
        return response.data().price();
    }
}
