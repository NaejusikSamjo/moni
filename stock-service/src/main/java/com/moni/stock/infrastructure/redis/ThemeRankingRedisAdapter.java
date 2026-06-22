package com.moni.stock.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.stock.presentation.dto.response.ThemeRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThemeRankingRedisAdapter {

    private static final String THEME_RANKING_KEY = "stock:theme:ranking";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(List<ThemeRankingResponse> rankings) {
        try {
            redisTemplate.opsForValue().set(
                    THEME_RANKING_KEY,
                    objectMapper.writeValueAsString(rankings),
                    TTL
            );
        } catch (JsonProcessingException ignored) {}
    }

    public List<ThemeRankingResponse> get() {
        String value = redisTemplate.opsForValue().get(THEME_RANKING_KEY);
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ThemeRankingResponse.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}