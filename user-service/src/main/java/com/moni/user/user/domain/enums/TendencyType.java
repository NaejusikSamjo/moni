package com.moni.user.user.domain.enums;

public enum TendencyType {
    AGGRESSIVE,      // 공격투자형
    ACTIVE,          // 적극투자형
    NEUTRAL,         // 위험중립형
    STABLE,          // 안정추구형
    SAFE;            // 안전형

    public static TendencyType fromScore(int score) {
        if (score >= 81) return AGGRESSIVE;
        if (score >= 61) return ACTIVE;
        if (score >= 41) return NEUTRAL;
        if (score >= 21) return STABLE;
        return SAFE;
    }
}