package com.moni.stock.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(example = "{\"tickers\": [\"005930\", \"000660\", \"035420\"]}")
public class BatchStockRequest {

    @Schema(example = "[\"005930\", \"000660\"]")
    private List<String> tickers = new ArrayList<>();

}
