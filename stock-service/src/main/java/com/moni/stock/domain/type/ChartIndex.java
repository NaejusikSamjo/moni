package com.moni.stock.domain.type;

import lombok.Getter;

@Getter
public enum ChartIndex {
    MIN_1("1"),
    MIN_3("3"),
    MIN_5("5"),
    MIN_10("10"),
    MIN_30("30"),
    MIN_60("60");

    private final String time;

    ChartIndex(String time) {
        this.time = time;
    }

}