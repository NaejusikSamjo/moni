package com.moni.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "p_market_news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketNewsEntity extends IdAudit {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "keyword", length = 50)
    private String keyword;

    @Builder
    public MarketNewsEntity(String title, String content, String source,
                            String url, LocalDateTime publishedAt, String keyword) {
        this.title = title;
        this.content = content;
        this.source = source;
        this.url = url;
        this.publishedAt = publishedAt;
        this.keyword = keyword;
    }
}