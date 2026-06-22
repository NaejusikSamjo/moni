package com.moni.stock.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeRankingResponse {
    private String themeCode;
    private String themeName;
    private String currentIndex;
    private String changeRate;
    private long volume;
}