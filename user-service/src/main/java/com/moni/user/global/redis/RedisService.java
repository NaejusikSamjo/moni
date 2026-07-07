package com.moni.user.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // GET-then-SET을 원자적으로 처리해 동시 요청 시 lost update를 방지
    private static final RedisScript<Long> COMPARE_AND_SET_SCRIPT = redisScript();

    private static RedisScript<Long> redisScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/refresh-token-cas.lua"));
        script.setResultType(Long.class);
        return script;
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    // 저장된 값이 expectedValue와 일치할 때만 newValue로 원자적 교체
    public CasResult compareAndSet(String key, String expectedValue, String newValue, Duration ttl) {
        Long result = redisTemplate.execute(
                COMPARE_AND_SET_SCRIPT,
                List.of(key),
                expectedValue,
                newValue,
                String.valueOf(ttl.toMillis())
        );
        return CasResult.from(result == null ? 0L : result);
    }
}