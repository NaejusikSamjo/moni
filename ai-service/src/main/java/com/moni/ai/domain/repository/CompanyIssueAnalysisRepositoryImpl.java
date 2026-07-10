package com.moni.ai.domain.repository;


import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.entity.QCompanyIssueAnalysisEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyIssueAnalysisRepositoryImpl implements CompanyIssueAnalysisRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    QCompanyIssueAnalysisEntity companyIssueAnalysis = QCompanyIssueAnalysisEntity.companyIssueAnalysisEntity;

    @Override
    public Optional<CompanyIssueAnalysisEntity> findLatestValidAnalysis(String ticker) {
        CompanyIssueAnalysisEntity result = queryFactory
                .selectFrom(companyIssueAnalysis)
                .where(
                        companyIssueAnalysis.ticker.eq(ticker),
                        companyIssueAnalysis.expiredAt.after(LocalDateTime.now()),
                        companyIssueAnalysis.deletedAt.isNull()
                )
                .orderBy(companyIssueAnalysis.createdAt.desc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }
}
