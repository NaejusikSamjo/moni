package com.moni.stock.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.stock.application.port.in.StockQueryUseCase;
import com.moni.stock.config.KisWebSocketInitializer;
import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.entity.StockPrice;
import com.moni.stock.domain.exception.StockErrorCode;
import com.moni.stock.domain.repository.StockRepository;
import com.moni.stock.domain.type.ChartIndex;
import com.moni.stock.infrastructure.client.KisOAuthClient;
import com.moni.stock.infrastructure.redis.StockPriceRedisAdapter;
import com.moni.stock.infrastructure.redis.ThemeRankingRedisAdapter;
import com.moni.stock.presentation.dto.response.StockChartResponse;
import com.moni.stock.presentation.dto.response.StockResDto;
import com.moni.stock.presentation.dto.response.ThemeRankingResponse;
import com.moni.stock.presentation.dto.response.TopVolumeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService implements StockQueryUseCase {

    private final StockRepository stockRepository;
    private final StockPriceRedisAdapter stockPriceRedisAdapter;
    private final KisWebSocketInitializer kisWebSocketInitializer;
    private final KisOAuthClient kisOAuthClient;
    private final ThemeRankingRedisAdapter themeRankingRedisAdapter;

    @Override
    public PageRes<StockResDto> getStockList(String keyword, Pageable pageable) {

        Page<Stock> stockPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            stockPage = stockRepository.findByNameContaining(keyword, pageable);
        } else {
            stockPage = stockRepository.findAll(pageable);
        }

        Page<StockResDto> dtoPage = stockPage.map(stock -> {
            kisWebSocketInitializer.wsSubscribe(stock.getTicker());
            BigDecimal currentPrice = stockPriceRedisAdapter.getPrice(stock.getTicker())
                    .map(StockPrice::getCurrentPrice)
                    .orElseGet(() -> fetchAndCacheCurrentPrice(stock.getTicker()));
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
                .map(StockPrice::getCurrentPrice)
                .orElseGet(() -> fetchAndCacheCurrentPrice(ticker));

        return new StockResDto(ticker, stock.getName(), price);
    }

    private BigDecimal fetchAndCacheCurrentPrice(String ticker) {
        try {
            JsonNode output = kisOAuthClient.getCurrentPrice(ticker).path("output");
            BigDecimal price = new BigDecimal(output.path("stck_prpr").asText("0"));
            StockPrice stockPrice = StockPrice.builder()
                    .ticker(ticker)
                    .currentPrice(price)
                    .askPrice(new BigDecimal(output.path("stck_askp").asText("0")))
                    .bidPrice(new BigDecimal(output.path("stck_bidp").asText("0")))
                    .volume(output.path("acml_vol").asLong())
                    .build();
            stockPriceRedisAdapter.savePrice(stockPrice);
            return price;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public StockChartResponse getChart(String ticker, ChartIndex index) {
        int intervalMin = Integer.parseInt(index.getTime());
        int callCount = (int) Math.ceil((double) intervalMin * 30 / 30); // 목표 30봉 기준

        List<StockChartResponse.CandleData> rawMinCandles = new ArrayList<>();
        String targetTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        for (int i = 0; i < callCount; i++) {
            JsonNode output2 = kisOAuthClient.getCandle(ticker, targetTime, index.getTime()).path("output2");
            if (!output2.isArray() || output2.isEmpty()) break;

            for (JsonNode node : output2) {
                String rawTime = node.path("stck_cntg_hour").asText();
                rawMinCandles.add(StockChartResponse.CandleData.builder()
                        .time(rawTime.substring(0, 2) + ":" + rawTime.substring(2, 4))
                        .open(node.path("stck_oprc").asInt())
                        .high(node.path("stck_hgpr").asInt())
                        .low(node.path("stck_lwpr").asInt())
                        .close(node.path("stck_prpr").asInt())
                        .volume(node.path("cntg_vol").asLong())
                        .build());
            }

            // 마지막 캔들 시간 - 1분 = 다음 호출 기준시간
            String lastRawTime = output2.get(output2.size() - 1).path("stck_cntg_hour").asText();
            targetTime = LocalTime.parse(lastRawTime, DateTimeFormatter.ofPattern("HHmmss"))
                    .minusMinutes(1)
                    .format(DateTimeFormatter.ofPattern("HHmmss"));
        }

        Collections.reverse(rawMinCandles);

        return StockChartResponse.builder()
                .type(index.getTime() + "min")
                .candles(aggregate(rawMinCandles, intervalMin))
                .build();
    }

    private List<StockChartResponse.CandleData> aggregate(
            List<StockChartResponse.CandleData> minCandles, int intervalMin) {
        if (intervalMin == 1) return minCandles;

        LinkedHashMap<String, List<StockChartResponse.CandleData>> grouped = new LinkedHashMap<>();
        for (StockChartResponse.CandleData candle : minCandles) {
            String[] parts = candle.getTime().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String key = String.format("%02d:%02d", hour, (minute / intervalMin) * intervalMin);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(candle);
        }

        List<StockChartResponse.CandleData> result = new ArrayList<>();
        for (Map.Entry<String, List<StockChartResponse.CandleData>> entry : grouped.entrySet()) {
            List<StockChartResponse.CandleData> group = entry.getValue();
            result.add(StockChartResponse.CandleData.builder()
                    .time(entry.getKey())
                    .open(group.get(0).getOpen())
                    .high(group.stream().mapToInt(StockChartResponse.CandleData::getHigh).max().orElse(0))
                    .low(group.stream().mapToInt(StockChartResponse.CandleData::getLow).min().orElse(0))
                    .close(group.get(group.size() - 1).getClose())
                    .volume(group.stream().mapToLong(StockChartResponse.CandleData::getVolume).sum())
                    .build());
        }
        return result;
    }

    @Override
    public List<ThemeRankingResponse> getThemes() {
        return themeRankingRedisAdapter.get();
    }

    @Override
    public TopVolumeResponse getTopVolume() {
        JsonNode output = kisOAuthClient.getVolumeRank().path("output");

        List<TopVolumeResponse.StockItem> stocks = new ArrayList<>();
        int rank = 1;
        for (JsonNode node : output) {
            if (rank <= 5) {
                stocks.add(TopVolumeResponse.StockItem.builder()
                        .rank(rank++)
                        .ticker(node.path("mksc_shrn_iscd").asText())
                        .name(node.path("hts_kor_isnm").asText())
                        .price(node.path("stck_prpr").asLong())
                        .volume(node.path("acml_vol").asLong())
                        .build());
            }

        }

        return TopVolumeResponse.builder()
                .stocks(stocks)
                .build();
    }


}