package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.enums.SentimentEnum;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class CompanyIssueResDto {

    @JsonProperty("ticker")
    public String ticker;

    @JsonProperty("company_name")
    public String companyName;

    @JsonProperty("summary")
    public String summary;

    @JsonProperty("sentiment")
    public SentimentEnum sentiment;

    @JsonProperty("analyzedAt")
    public LocalDateTime analyzedAt;

    public static CompanyIssueResDto toDto(CompanyIssueAnalysisEntity entity){
        return CompanyIssueResDto.builder()
                .ticker(entity.getTicker())
                .companyName(entity.getCompanyName())
                .summary(entity.getSummary())
                .sentiment(entity.getSentiment())
                .analyzedAt(entity.getCreatedAt())
        .build();
    }
}
