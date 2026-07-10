package com.moni.ai.domain.enums;

import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.common.error.exception.CustomException;
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


    public static MarketKeyword fromKeyword(String keyword){
        return Arrays.stream(values())
                .filter(c -> c.getKeyword().equals(keyword))
                .findFirst()
                .orElseThrow(() -> new CustomException(AiErrorCode.MARKET_KEYWORD_MISMATCH));
    }
}
