package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moni.ai.domain.enums.WatchCompany;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatchCompanyResDto {

    @JsonProperty("ticker")
    private String ticker;

    @JsonProperty("company_name")
    private String companyName;


    public static WatchCompanyResDto from(WatchCompany watchCompany) {
        return WatchCompanyResDto.builder()
                .ticker(watchCompany.getTicker())
                .companyName(watchCompany.getCompanyName())
                .build();
    }
}
