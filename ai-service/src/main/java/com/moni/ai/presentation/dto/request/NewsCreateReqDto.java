package com.moni.ai.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Builder
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsCreateReqDto {

    @NotBlank(message = "ticker는 필수입니다.")
    @Size(max = 6, message = "ticker는 6자 이내여야 합니다.")
    @JsonProperty("ticker")
    private String ticker;

    @NotBlank(message = "title은 필수입니다.")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "company_name은 필수입니다.")
    @JsonProperty("company_name")
    private String companyName;

    @NotBlank(message = "content는 필수입니다.")
    @JsonProperty("content")
    private String content;

    @NotBlank(message = "source는 필수입니다.")
    @Size(max=50,message="source는 50자 이내여야 합니다.")
    @JsonProperty("source")
    private String source;

    @NotBlank(message = "url은 필수입니다.")
    @JsonProperty("url")
    private String url;

    @NotNull(message = "published_at은 필수입니다.")
    @JsonProperty("published_at")
    private LocalDateTime publishedAt;
}
