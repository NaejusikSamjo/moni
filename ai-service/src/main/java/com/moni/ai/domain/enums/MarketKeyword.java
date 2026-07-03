package com.moni.ai.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum MarketKeyword {
    INDEX("코스피"),
    NASDAQ("나스닥"),
    WAR("전쟁"),
    DOLLAR("달러"),
    AI("AI 투자"),
    FED("연준"),
    INTEREST_RATE("금리"),
    OIL("유가"),
    SEMICONDUCTOR("반도체"),
    INFLATION("인플레이션");

    private final String keyword;

    MarketKeyword(String keyword) {
        this.keyword = keyword;
    }

    public static List<String> getAllKeywords() {
        return Arrays.stream(values())
                .map(MarketKeyword::getKeyword)
                .toList();
    }
}
