package com.moni.trade.reservedorder.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.exception.HoldingErrorCode;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.reservedorder.domain.entity.ReservedOrder;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderStatus;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import com.moni.trade.reservedorder.domain.exception.ReservedOrderErrorCode;
import com.moni.trade.reservedorder.domain.repository.ReservedOrderRepository;
import com.moni.trade.reservedorder.presentation.dto.request.ReservedBuyOrderRequestDto;
import com.moni.trade.reservedorder.presentation.dto.request.ReservedSellOrderRequestDto;
import com.moni.trade.reservedorder.presentation.dto.response.ReservedOrderResponseDto;
import com.moni.trade.trade.application.service.TradeExecutor;
import com.moni.trade.trade.domain.entity.Trade;
import com.moni.trade.trade.domain.enums.TradeType;
import com.moni.trade.trade.domain.repository.TradeRepository;
import com.moni.trade.trade.infrastructure.message.event.TradeCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservedOrderService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final ReservedOrderRepository reservedOrderRepository;
    private final TradeExecutor tradeExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Lazy
    @Autowired
    private ReservedOrderService self;

    @Transactional
    public ReservedOrderResponseDto createBuyOrder(UUID userId, ReservedBuyOrderRequestDto request) {
        if (request.orderType() == ReservedOrderType.LIMIT && request.targetPrice() == null) {
            throw new CustomException(ReservedOrderErrorCode.INVALID_ORDER_REQUEST);
        }

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        ReservedOrder order;
        if (request.orderType() == ReservedOrderType.LIMIT) {
            order = ReservedOrder.createLimitBuy(account.getId(), request.ticker(),
                    request.targetPrice(), request.amount());
        } else {
            order = ReservedOrder.createReservationBuy(account.getId(), request.ticker(), request.amount());
        }

        return ReservedOrderResponseDto.from(reservedOrderRepository.save(order));
    }

    @Transactional
    public ReservedOrderResponseDto createSellOrder(UUID userId, ReservedSellOrderRequestDto request) {
        if (request.orderType() == ReservedOrderType.LIMIT && request.targetPrice() == null) {
            throw new CustomException(ReservedOrderErrorCode.INVALID_ORDER_REQUEST);
        }

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        ReservedOrder order;
        if (request.orderType() == ReservedOrderType.LIMIT) {
            order = ReservedOrder.createLimitSell(account.getId(), request.ticker(),
                    request.targetPrice(), request.quantity());
        } else {
            order = ReservedOrder.createReservationSell(account.getId(), request.ticker(), request.quantity());
        }

        return ReservedOrderResponseDto.from(reservedOrderRepository.save(order));
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        ReservedOrder order = reservedOrderRepository.findByIdAndAccountId(orderId, account.getId())
                .orElseThrow(() -> new CustomException(ReservedOrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != ReservedOrderStatus.PENDING) {
            throw new CustomException(ReservedOrderErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        order.cancel();
    }

    public List<ReservedOrderResponseDto> findMyOrders(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        return reservedOrderRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(ReservedOrderResponseDto::from)
                .toList();
    }

    public void processReservationOrders() {
        List<ReservedOrder> pendingOrders = reservedOrderRepository
                .findByOrderTypeAndStatusOrderByCreatedAtAsc(
                        ReservedOrderType.RESERVATION, ReservedOrderStatus.PENDING);

        for (ReservedOrder order : pendingOrders) {
            try {
                self.executeOrder(order.getId());
            } catch (Exception e) {
                log.error("예약 주문 처리 실패 orderId={}", order.getId(), e);
            }
        }
    }

    public void processLimitOrders() {
        List<ReservedOrder> pendingOrders = reservedOrderRepository
                .findByOrderTypeAndStatusOrderByCreatedAtAsc(
                        ReservedOrderType.LIMIT, ReservedOrderStatus.PENDING);

        for (ReservedOrder order : pendingOrders) {
            try {
                BigDecimal currentPrice = tradeExecutor.getCurrentPrice(order.getTicker());
                if (order.isLimitConditionMet(currentPrice)) {
                    self.executeOrder(order.getId());
                }
            } catch (Exception e) {
                log.error("지정가 주문 처리 실패 orderId={}", order.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeOrder(UUID orderId) {
        ReservedOrder order = reservedOrderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ReservedOrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != ReservedOrderStatus.PENDING) {
            return;
        }

        Account account = accountRepository.findByIdWithLock(order.getAccountId())
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        try {
            BigDecimal currentPrice = tradeExecutor.getCurrentPrice(order.getTicker());

            if (order.getTradeType() == TradeType.BUY) {
                executeBuyOrder(order, account, currentPrice);
            } else {
                executeSellOrder(order, account, currentPrice);
            }

            order.execute();
        } catch (CustomException e) {
            log.warn("예약 주문 자동 취소 orderId={} reason={}", orderId, e.getMessage());
            order.cancel();
        } catch (Exception e) {
            log.error("예약 주문 실패 orderId={}", orderId, e);
            order.fail();
        }
    }

    private void executeBuyOrder(ReservedOrder order, Account account, BigDecimal currentPrice) {
        BigDecimal quantity = order.getAmount().divide(currentPrice, 2, RoundingMode.DOWN);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(AccountErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal actualCost = quantity.multiply(currentPrice).setScale(2, RoundingMode.DOWN);
        if (account.getBalance().compareTo(actualCost) < 0) {
            throw new CustomException(AccountErrorCode.INSUFFICIENT_BALANCE);
        }

        Trade trade = Trade.createBuy(account.getId(), order.getTicker(), quantity, currentPrice);
        tradeRepository.save(trade);
        account.deductBalance(actualCost);

        holdingRepository.findByAccountIdAndTicker(account.getId(), order.getTicker())
                .ifPresentOrElse(
                        holding -> holding.buy(quantity, currentPrice),
                        () -> holdingRepository.save(
                                Holding.create(account.getId(), order.getTicker(), quantity, currentPrice)
                        )
                );

        trade.complete();
        applicationEventPublisher.publishEvent(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), null, null
        ));
    }

    private void executeSellOrder(ReservedOrder order, Account account, BigDecimal currentPrice) {
        Holding holding = holdingRepository
                .findByAccountIdAndTicker(account.getId(), order.getTicker())
                .orElseThrow(() -> new CustomException(HoldingErrorCode.HOLDING_NOT_FOUND));

        if (holding.getQuantity().compareTo(order.getQuantity()) < 0) {
            throw new CustomException(HoldingErrorCode.INSUFFICIENT_QUANTITY);
        }

        BigDecimal quantity = order.getQuantity();
        BigDecimal totalAmount = currentPrice.multiply(quantity).setScale(2, RoundingMode.DOWN);
        BigDecimal profitAmount = currentPrice.subtract(holding.getAveragePrice())
                .multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitRate = currentPrice.subtract(holding.getAveragePrice())
                .divide(holding.getAveragePrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        Trade trade = Trade.createSell(account.getId(), order.getTicker(), quantity,
                currentPrice, profitAmount, profitRate);
        tradeRepository.save(trade);

        account.addBalance(totalAmount);
        holding.sell(quantity);
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        }

        trade.complete();
        applicationEventPublisher.publishEvent(new TradeCompletedEvent(
                trade.getId(), account.getId(), trade.getTicker(),
                trade.getTradeType(), trade.getQuantity(), trade.getPrice(),
                trade.getTotalAmount(), profitAmount, profitRate
        ));
    }
}
