package com.moni.stock.infrastructure.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.repository.ThemeRepository;
import com.moni.stock.infrastructure.client.KisOAuthClient;
import com.moni.stock.infrastructure.redis.ThemeRankingRedisAdapter;
import com.moni.stock.presentation.dto.response.ThemeRankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeRankingScheduler {

    private static final int TOP_N = 5;
    private static final long CALL_INTERVAL_MS = 300L; // 18회/초 제한 준수 (~16.6회/초)

    private final ThemeRepository themeRepository;
    private final KisOAuthClient kisOAuthClient;
    private final ThemeRankingRedisAdapter themeRankingRedisAdapter;

    @Scheduled(fixedDelay = 60000)
    public void updateThemeRanking() {
        List<Theme> themes = themeRepository.findAll();
        List<ThemeRankingResponse> rankings = new ArrayList<>();

        for (Theme theme : themes) {
            try {
                JsonNode output = kisOAuthClient.getThemeInfo(theme.getThemeCode()).path("output");
                rankings.add(ThemeRankingResponse.builder()
                        .themeCode(theme.getThemeCode())
                        .themeName(theme.getThemeName())
                        .currentIndex(output.path("bstp_nmix_prpr").asText())
                        .changeRate(output.path("bstp_nmix_prdy_ctrt").asText())
                        .volume(output.path("acml_vol").asLong())
                        .build());
                Thread.sleep(CALL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("테마 랭킹 스케줄러 인터럽트");
                break;
            } catch (Exception e) {
                    log.warn("테마 조회 실패 - code: {}, error: {}", theme.getThemeCode(), e.getMessage(), e);
                }
        }

        List<ThemeRankingResponse> top5 = rankings.stream()
                .sorted(Comparator.comparingLong(ThemeRankingResponse::getVolume).reversed())
                .limit(TOP_N)
                .toList();

        themeRankingRedisAdapter.save(top5);
        log.info("테마 랭킹 업데이트 완료 - {}개 조회, 상위 {}개 캐시", rankings.size(), top5.size());
    }
}