package com.moni.stock.infrastructure.redis;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

// 로컬 moni_redis 컨테이너(단일 Redis) 대상 — 실행 전 docker-compose.infra.yml로 로컬에 띄워둘 것
@Tag("benchmark")
@TestPropertySource(properties = {
        "stock.redis.mode=standalone",
        "stock.redis.standalone.host=localhost",
        "stock.redis.standalone.port=26379"
})
class StockRedisStandaloneBenchmarkTest extends StockPriceWriteBenchmarkSupport {
}