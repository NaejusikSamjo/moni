package com.moni.ai.presentation.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.moni.ai.domain.entity.NewsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResDto {

    @JsonProperty("title")
    private String title;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("source")
    private String source;

    public static NewsResDto from(NewsEntity entity) {
        return NewsResDto.builder()
                .title(entity.getTitle())
                .publishedAt(entity.getPublishedAt())
                .source(entity.getSource())
                .build();
    }
}
