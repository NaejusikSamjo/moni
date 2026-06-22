package com.moni.stock;

import com.moni.stock.infrastructure.client.KisOAuthClient;
import com.moni.stock.infrastructure.redis.StockPriceRedisAdapter;
import com.moni.stock.infrastructure.redis.ThemeRankingRedisAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class StockServiceApplicationTest {

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private KisOAuthClient kisOAuthClient;

    @MockitoBean
    private StockPriceRedisAdapter stockPriceRedisAdapter;

    @MockitoBean
    private ThemeRankingRedisAdapter themeRankingRedisAdapter;

    @Test
    void contextLoads() {
    }
}