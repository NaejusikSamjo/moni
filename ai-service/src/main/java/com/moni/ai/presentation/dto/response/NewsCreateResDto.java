package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsCreateResDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("ticker")
    private String ticker;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("content")
    private String content;

    @JsonProperty("source")
    private String source;

    @JsonProperty("url")
    private String url;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static NewsCreateResDto from(NewsEntity newsEntity){
        return NewsCreateResDto.builder()
                .id(newsEntity.getId())
                .ticker(newsEntity.getTicker())
                .title(newsEntity.getTitle())
                .companyName(newsEntity.getCompanyName())
                .content(newsEntity.getContent())
                .source(newsEntity.getSource())
                .url(newsEntity.getUrl())
                .publishedAt(newsEntity.getPublishedAt())
                .createdAt(newsEntity.getCreatedAt())
                .build();
    }
}
