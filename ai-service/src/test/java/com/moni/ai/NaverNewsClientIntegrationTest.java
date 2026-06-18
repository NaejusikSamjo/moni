package com.moni.ai;

import com.moni.ai.application.service.NewsCollectService;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.response.NaverNewsResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@WithMockUser
class NaverNewsClientIntegrationTest {

    @Autowired
    private NaverNewsClient naverNewsClient;

    @Autowired
    private NewsCollectService newsCollectService;

    @Autowired
    private NewsRepository newsRepository;

    @Test
    @DisplayName("DB저장 확인")
    void API_호출_정상_반환() {
        // when

        newsCollectService.collectByTicker("005930","삼성전자");

        // then
        List<NewsEntity> saved = newsRepository.findByTicker("005930");
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getTicker()).isEqualTo("005930");

        // 저장된 내용 로그로 확인
        saved.forEach(news ->
                log.info("저장된 뉴스 - 제목: {}, 날짜: {}", news.getTitle(), news.getPublishedAt())
        );

    }

    @Test
    @DisplayName("HTML 태그와 엔티티가 정제된 텍스트가 반환된다")
    void HTML_태그_정제_확인() {
        // when
        List<NaverNewsResponse.NaverNewsItem> items =
                naverNewsClient.fetchNews("삼성전자 실적", 5, "sim");

        // then
        items.forEach(item -> {
            assertThat(item.getCleanTitle()).doesNotContain("<b>", "</b>");
            assertThat(item.getCleanDescription()).doesNotContain("<b>", "</b>");
            assertThat(item.getCleanTitle()).doesNotContain("&quot;", "&amp;");
        });
    }
}
