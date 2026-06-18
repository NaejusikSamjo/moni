package com.moni.ai;

import com.moni.ai.application.service.NewsCollectService;
import com.moni.ai.application.service.NewsFilterService;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.response.NaverNewsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("News Collection Service 단위 테스트")
public class NewsCollectionTest {

    @InjectMocks
    private NewsCollectService newsCollectService;

    @Mock
    private NewsFilterService newsFilterService;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NaverNewsClient naverNewsClient;

    @Test
    @DisplayName("필터를 모두 통과한 기사는 저장된다")
    void filterSuccessSaveTest() {
        // given
        NaverNewsResponse.NaverNewsItem item = mockItem(
                "삼성전자 실적 발표",
                "삼성전자가 역대 최대 실적을 기록했다.",
                "https://news.naver.com/article/001",
                LocalDateTime.now().minusDays(1)
        );

        when(naverNewsClient.fetchNews(anyString(), anyInt(), anyString()))
                .thenReturn(List.of(item));
        when(newsRepository.existsByUrl(anyString())).thenReturn(false);
        when(newsFilterService.isWithinDays(any())).thenReturn(true);
        when(newsFilterService.isRelevant(any(), anyString())).thenReturn(true);
        when(newsFilterService.isKeywordNearCompany(anyString(), anyString(), anyString())).thenReturn(true);

        // when
        newsCollectService.collectByTicker("005930", "삼성전자");

        // then
        verify(newsRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("필터에서 걸린 기사는 저장되지 않는다")
    void filterFailSaveTest() {
        // given
        NaverNewsResponse.NaverNewsItem item = mockItem(
                "쿠팡 과징금 부과, 삼성전자 다음으로 고용 큰 기업",
                "공정위가 쿠팡에 과징금을 부과했다. 삼성전자 다음으로...",
                "https://news.naver.com/article/002",
                LocalDateTime.now().minusDays(1)
        );

        when(naverNewsClient.fetchNews(anyString(), anyInt(), anyString()))
                .thenReturn(List.of(item));
        when(newsRepository.existsByUrl(anyString())).thenReturn(false);
        when(newsFilterService.isWithinDays(any())).thenReturn(true);
        when(newsFilterService.isRelevant(any(), anyString())).thenReturn(false); // 기업명 필터에서 탈락

        // when
        newsCollectService.collectByTicker("005930", "삼성전자");

        // then
        verify(newsRepository, never()).save(any());
    }

    // NaverNewsResponse.NaverNewsItem mock 생성 헬퍼
    private NaverNewsResponse.NaverNewsItem mockItem(
            String title, String description, String link, LocalDateTime pubDate) {

        NaverNewsResponse.NaverNewsItem item = mock(NaverNewsResponse.NaverNewsItem.class);
        when(item.getCleanTitle()).thenReturn(title);
        when(item.getCleanDescription()).thenReturn(description);
        when(item.getLink()).thenReturn(link);
        when(item.getOriginallink()).thenReturn(link);
        when(item.getParsedPubDate()).thenReturn(pubDate);
        return item;
    }
}
