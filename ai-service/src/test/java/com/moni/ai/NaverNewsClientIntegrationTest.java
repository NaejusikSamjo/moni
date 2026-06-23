package com.moni.ai;

import com.moni.ai.application.service.AiService;
import com.moni.ai.application.service.NewsCollectService;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.repository.NewsRepository;
import com.moni.ai.infrastructure.client.NaverNewsClient;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.ai.presentation.dto.response.NaverNewsResDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
@Disabled("통합테스트 - 로컬에서만 수동 실행")
class NaverNewsClientIntegrationTest {

    @Autowired
    private NaverNewsClient naverNewsClient;

    @Autowired
    private NewsCollectService newsCollectService;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AiService aiService;

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
        List<NaverNewsResDto.NaverNewsItem> items =
                naverNewsClient.fetchNews("삼성전자 실적", 5, "sim");

        // then
        items.forEach(item -> {
            assertThat(item.getCleanTitle()).doesNotContain("<b>", "</b>");
            assertThat(item.getCleanDescription()).doesNotContain("<b>", "</b>");
            assertThat(item.getCleanTitle()).doesNotContain("&quot;", "&amp;");
        });
    }

    @Test
    @DisplayName("삼성전자 관련 뉴스 벡터 검색")
    void 삼성전자_벡터_검색() {
        // when
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("삼성전자 실적 전망")
                        .topK(5)
                        .filterExpression("ticker == '005930'")
                        .build()
        );

        // then
        assertThat(results).isNotEmpty();
        results.forEach(doc -> {
            log.info("유사도 검색 결과 - 내용: {}", doc.getText());
            log.info("메타데이터: {}", doc.getMetadata());
        });
    }

    @Test
    @DisplayName("삼성전자 AI 분석 결과 반환 확인")
    void 삼성전자_AI_분석() {
        // when
        CompanyIssueResDto result = aiService.analyze("005930", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ticker).isEqualTo("005930");
        assertThat(result.summary).isNotBlank();
        assertThat(result.sentiment).isNotNull();

        log.info("분석 결과 - ticker: {}", result.ticker);
        log.info("분석 결과 - companyName: {}", result.companyName);
        log.info("분석 결과 - sentiment: {}", result.sentiment);
        log.info("분석 결과 - summary: {}", result.summary);
    }
}
