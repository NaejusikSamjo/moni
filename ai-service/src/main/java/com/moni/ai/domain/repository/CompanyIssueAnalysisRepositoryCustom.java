package com.moni.ai.domain.repository;


import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;

import java.util.Optional;

public interface CompanyIssueAnalysisRepositoryCustom {
    Optional<CompanyIssueAnalysisEntity> findLatestValidAnalysis(String ticker);
}
