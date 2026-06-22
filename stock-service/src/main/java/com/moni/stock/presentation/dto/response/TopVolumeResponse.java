package com.moni.stock.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopVolumeResponse {
    private List<StockItem> stocks;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private int rank;
        private String ticker;
        private String name;
        private long price;
        private long volume;
    }
}