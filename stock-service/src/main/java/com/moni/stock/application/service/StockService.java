package com.moni.stock.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.stock.application.port.in.StockQueryUseCase;
import com.moni.stock.config.KisWebSocketInitializer;
import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.entity.StockPrice;
import com.moni.stock.domain.exception.StockErrorCode;
import com.moni.stock.domain.repository.StockRepository;
import com.moni.stock.domain.type.MarketType;
import com.moni.stock.infrastructure.redis.StockPriceRedisAdapter;
import com.moni.stock.presentation.dto.response.StockResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService implements StockQueryUseCase {

    private final StockRepository stockRepository;
    private final StockPriceRedisAdapter stockPriceRedisAdapter;
    private final KisWebSocketInitializer kisWebSocketInitializer;

    @Override
    public PageRes<StockResDto> getStockList(String keyword, Pageable pageable) {

        Page<Stock> stockPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            stockPage = stockRepository.findByNameContaining(keyword, pageable);
        } else {
            stockPage = stockRepository.findAll(pageable);
        }

        Page<StockResDto> dtoPage = stockPage.map(stock -> {

            // 웹소켓 구독
            kisWebSocketInitializer.wsSubscribe(stock.getTicker());

            // 레디스에서 가격 가져오기 (없으면 0원)
            BigDecimal currentPrice = stockPriceRedisAdapter.getPrice(stock.getTicker())
                    .map(StockPrice::getCurrentPrice)
                    .orElse(BigDecimal.ZERO);

            return new StockResDto(stock.getTicker(), stock.getName(), currentPrice);
        });

        return new PageRes<>(dtoPage);


    }

    @Override
    public StockResDto getStockDetail(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));

        kisWebSocketInitializer.wsSubscribe(stock.getTicker());

        BigDecimal price = stockPriceRedisAdapter.getPrice(ticker)
                .map(stockPrice -> stockPrice.getCurrentPrice())
                .orElse(BigDecimal.ZERO);

        return new StockResDto(ticker, stock.getName(), price);
    }


}