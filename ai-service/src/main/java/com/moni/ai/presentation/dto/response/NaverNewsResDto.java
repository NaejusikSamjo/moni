package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverNewsResDto {

    private int total;
    private int start;
    private int display;
    private List<NaverNewsItem> items;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverNewsItem {

        private String title;
        private String link;
        private String originallink;
        private String description;
        private String pubDate;


        // HTML 엔티티 디코딩 추가
        public String getCleanTitle() {
            if (title == null) return "";
            return title.replaceAll("<[^>]*>", "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
        }

        // HTML 엔티티 디코딩 추가
        public String getCleanDescription() {
            if (description == null) return "";
            return description.replaceAll("<[^>]*>", "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
        }

        public LocalDateTime getParsedPubDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH
            );
            return ZonedDateTime.parse(pubDate, formatter)
                    .toLocalDateTime();
        }
    }
}