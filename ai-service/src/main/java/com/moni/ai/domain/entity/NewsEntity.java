package com.moni.ai.domain.entity;

import com.fasterxml.uuid.Generators;
import com.moni.common.JpaAuditing.baseEntity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="p_news")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class NewsEntity extends BaseEntity{

    @Id
    private UUID id;

    @Column(name="ticker",length=10)
    private String ticker;

    @Column(name="title",nullable=false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name="source",length=50,nullable = false)
    private String source;

    @Column(name="url",nullable = false)
    private String url;


    @Column(name="published_at",nullable = false)
    private LocalDateTime publishedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
    }

    @Builder
    public NewsEntity(
            String ticker,
            String title,
            String content,
            String source,
            String url,
            LocalDateTime publishedAt
    ){
        this.ticker = ticker;
        this.title = title;
        this.content = content;
        this.source = source;
        this.url = url;
        this.publishedAt = publishedAt;
    }


}
