package com.moni.ai.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ImpactKeyword {
    EARNINGS("실적"),
    ORDER("수주"),
    CONTRACT("계약"),
    MA("M&A"),
    ACQUISITION("인수"),
    LAWSUIT("소송"),
    PENALTY("과징금"),
    EARNING_SHOCK("어닝쇼크"),
    TURN_PROFIT("흑자전환"),
    TURN_LOSS("적자전환"),
    RESTRUCTURING("구조조정"),
    RIGHTS_OFFERING("유상증자");

    private final String keyword;

    ImpactKeyword(String keyword) {
        this.keyword = keyword;
    }

    public static List<String> getAllKeywords() {
        return Arrays.stream(values())
                .map(ImpactKeyword::getKeyword)
                .toList();
    }
}
