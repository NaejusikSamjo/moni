package com.moni.stock.application.port.in;

import com.moni.common.response.paging.PageRes;
import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.type.ChartType;
import com.moni.stock.presentation.dto.response.StockResDto;
import org.springframework.data.domain.Pageable;

public interface StockQueryUseCase {

    PageRes<StockResDto> getStockList(String keyword, Pageable pageable);

    StockResDto getStockDetail(String ticker);

}