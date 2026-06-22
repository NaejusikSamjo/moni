package com.moni.stock.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class StockChartResponse {
    private String type; // "1min", "5min" 등
    private List<CandleData> candles;

    @Getter
    @Builder
    public static class CandleData {
        private String time;   // "11:50" 형태로 변환된 시간
        private int open;      // 시가 (stck_oprc)
        private int high;      // 고가 (stck_hgpr)
        private int low;       // 저가 (stck_lwpr)
        private int close;     // 종가 (stck_prpr)
        private long volume;   // 체결 거래량 (cntg_vol)
    }
}