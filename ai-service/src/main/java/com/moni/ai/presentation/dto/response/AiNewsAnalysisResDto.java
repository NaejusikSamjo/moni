package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiNewsAnalysisResDto {
    private String summary;
    private String sentiment;
}
