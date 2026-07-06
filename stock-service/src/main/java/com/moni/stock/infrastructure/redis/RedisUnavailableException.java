package com.moni.stock.infrastructure.redis;

public class RedisUnavailableException extends RuntimeException {

    public RedisUnavailableException(String ticker, Throwable cause) {
        super("Redis 캐시를 사용할 수 없습니다. ticker=" + ticker, cause);
    }
}
