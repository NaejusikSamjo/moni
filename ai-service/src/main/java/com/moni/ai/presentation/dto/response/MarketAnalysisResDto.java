package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
public class MarketAnalysisResDto {
    private String summary;

    public static MarketAnalysisResDto from(
            MarketNewsAnalysisEntity entity
    ){
        return MarketAnalysisResDto.builder().
                summary(entity.getSummary())
                .build();
    }
}
