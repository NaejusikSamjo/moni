package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.domain.entity.QNewsEntity;
import com.moni.ai.presentation.dto.request.NewsSearchReqDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private static final QNewsEntity news = QNewsEntity.newsEntity;

    @Override
    public Page<NewsEntity> searchNews(NewsSearchReqDto request, Pageable pageable) {

        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        // 해당 날짜만 조회
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();

        List<NewsEntity> content = queryFactory
                .selectFrom(news)
                .where(
                        tickerEq(request.getTicker()),
                        companyNameEq(request.getCompanyName()),
                        keywordContains(request.getKeyword()),
                        news.publishedAt.between(from, to),
                        news.deletedAt.isNull()
                )
                .orderBy(news.publishedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(news.count())
                .from(news)
                .where(
                        tickerEq(request.getTicker()),
                        companyNameEq(request.getCompanyName()),
                        keywordContains(request.getKeyword()),
                        news.publishedAt.between(from, to),
                        news.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanExpression tickerEq(String ticker) {
        return ticker != null ? news.ticker.eq(ticker) : null;
    }

    private BooleanExpression companyNameEq(String companyName) {
        return companyName != null ? news.companyName.eq(companyName) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return keyword != null ? news.title.containsIgnoreCase(keyword) : null;
    }
}
