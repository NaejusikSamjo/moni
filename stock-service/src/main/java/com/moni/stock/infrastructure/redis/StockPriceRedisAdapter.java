package com.moni.stock.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.stock.domain.entity.StockPrice;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceRedisAdapter {

    private static final String PRICE_KEY_PREFIX = "stock:price:";
    private static final String TOP_VOLUME_KEY = "stock:top-volume";
    private static final Duration PRICE_TTL = Duration.ofSeconds(10);
    private static final Duration TOP_VOLUME_TTL = Duration.ofMinutes(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void savePrice(StockPrice stockPrice) {
        try {
            String key = PRICE_KEY_PREFIX + stockPrice.getTicker();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(stockPrice), PRICE_TTL);
        } catch (JsonProcessingException ignored) {
        } catch (DataAccessException e) {
            // 캐시 저장은 best-effort: Redis가 죽어있어도 이미 KIS에서 받아온 가격 자체는 그대로 응답해야 함
            log.warn("Redis 캐시 저장 실패, ticker={}", stockPrice.getTicker(), e);
        }
    }

    @CircuitBreaker(name = "redisPrice", fallbackMethod = "getPriceFallback")
    public Optional<StockPrice> getPrice(String ticker) {
        String value = redisTemplate.opsForValue().get(PRICE_KEY_PREFIX + ticker);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, StockPrice.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private Optional<StockPrice> getPriceFallback(String ticker, Throwable t) {
        throw new RedisUnavailableException(ticker, t);
    }

    public void saveTopVolume(List<StockPrice> stockPrices) {
        try {
            redisTemplate.opsForValue().set(TOP_VOLUME_KEY, objectMapper.writeValueAsString(stockPrices), TOP_VOLUME_TTL);
        } catch (JsonProcessingException ignored) {}
    }

    public List<StockPrice> getTopVolume() {
        String value = redisTemplate.opsForValue().get(TOP_VOLUME_KEY);
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, StockPrice.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}