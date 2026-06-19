package com.moni.ai.application.service;

import com.moni.ai.presentation.dto.response.NaverNewsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NewsFilterService {

    // 3일이내 작성된 기사인지 확인
    public boolean isWithinDays(NaverNewsResponse.NaverNewsItem item) {
        return item.getParsedPubDate()
                .isAfter(LocalDateTime.now().minusDays(3));
    }

    // 제목에 기업명 포함 여부
    public boolean isRelevant(NaverNewsResponse.NaverNewsItem item, String companyName) {
        String title = item.getCleanTitle();
        String description = item.getCleanDescription();

        // 제목에 기업명 없으면 관련도 낮음
        if (!title.contains(companyName)) {
            // description 첫 50자 안에도 없으면 제외
            String previewText = description.substring(0, Math.min(50, description.length()));
            if (!previewText.contains(companyName)) {
                return false;
            }
        }
        return true;
    }

    // 기업명과 키워드 사이가 얼마나 떨어져있는지
    public boolean isKeywordNearCompany(String text, String companyName, String keyword) {
        int companyIdx = text.indexOf(companyName);
        int keywordIdx = text.indexOf(keyword);

        if (companyIdx == -1 || keywordIdx == -1) return false;

        // 두 단어가 100자 이내에 있어야 관련 기사로 판단
        return Math.abs(companyIdx - keywordIdx) <= 100;
    }
}
