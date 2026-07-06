package com.moni.stock.application.port.in;

import com.moni.common.response.paging.PageRes;
import com.moni.stock.domain.type.ChartIndex;
import com.moni.stock.presentation.dto.request.BatchStockRequest;
import com.moni.stock.presentation.dto.response.StockChartResponse;
import com.moni.stock.presentation.dto.response.StockResDto;
import com.moni.stock.presentation.dto.response.ThemeRankingResponse;
import com.moni.stock.presentation.dto.response.TopVolumeResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockQueryUseCase {

    PageRes<StockResDto> getStockList(String keyword, Pageable pageable);

    StockResDto getStockDetail(String ticker);

    StockChartResponse getChart(String ticker, ChartIndex index);

    List<ThemeRankingResponse> getThemes();

    TopVolumeResponse getTopVolume();

    List<StockResDto> getStockDetailList(BatchStockRequest tickers);
}